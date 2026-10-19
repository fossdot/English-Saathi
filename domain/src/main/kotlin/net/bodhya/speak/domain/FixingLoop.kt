package net.bodhya.speak.domain

/**
 * Where a learner is in the 5-step fixing loop for one word of one sentence.
 *
 * Steps: 1 hear the word slowly, 2 see the syllables and the Hindi sound hint,
 * 3 say the word alone, 4 say the [chunk] containing it, 5 say the full
 * sentence. [fails] counts failed recordings at the current step.
 */
data class FixState(
    val sentenceId: String,
    val word: String,
    /** Position of [word] among the sentence's words. */
    val wordIndex: Int,
    val chunk: String,
    /** Position of [word] among the chunk's words. */
    val chunkWordIndex: Int,
    val sentenceText: String,
    val step: Int = 1,
    val fails: Int = 0,
) {
    init {
        require(word.isNotBlank()) { "no word to fix" }
        require(wordIndex >= 0 && chunkWordIndex >= 0) { "word positions can't be negative" }
        require(step in 1..FixingLoop.STEPS) { "step $step is not 1–${FixingLoop.STEPS}" }
        require(fails >= 0) { "fails can't be negative" }
    }
}

enum class FixOutcome {
    /** Step 3 or 4 passed; [FixState.step] moved on. */
    PASSED,

    /** Step 5 passed: the sentence is fixed and the loop is over. */
    SENTENCE_FIXED,

    /** Failed, fewer than [GameConfig.fixFailsBeforeDropBack] times in a row: try the same step again. */
    TRY_AGAIN,

    /** Failed [GameConfig.fixFailsBeforeDropBack] times in a row: [FixState.step] moved back one step. */
    DROPPED_BACK,

    /** No words were heard: nothing changes and it does not count as a failed try. */
    NOTHING_HEARD,
}

/**
 * Result of one recording in steps 3–5: the new [state], the [outcome], how
 * the step's target was said ([alignment], null when nothing was heard) and
 * the [xp] earned.
 */
data class FixResult(val state: FixState, val outcome: FixOutcome, val alignment: Alignment?, val xp: Int)

/** The fixing-loop state machine: the pilot's `startFix()` and `handleFix()` with the agreed changes. */
object FixingLoop {
    const val STEPS = 5
    private const val FIRST_RECORDING_STEP = 3

    /**
     * Starts fixing the word at [wordIndex] of [sentence]'s words. Step 4 uses
     * the chunk around that position. Chunks are located in order among the
     * sentence's words; chunks with no words are skipped. If the chunks can't
     * all be located, the first chunk that contains the word is used; if none
     * does, the full sentence.
     */
    fun start(sentence: Sentence, wordIndex: Int): FixState {
        val words = sentence.targetWords
        require(wordIndex in words.indices) { "word index $wordIndex out of range for \"${sentence.text}\"" }
        val (chunk, chunkWordIndex) = chunkAt(sentence, words, wordIndex)
        return FixState(sentence.id, words[wordIndex], wordIndex, chunk, chunkWordIndex, sentence.text)
    }

    /** Steps 3–5 are recorded and checked; steps 1–2 only need "Next". */
    fun isRecordingStep(step: Int): Boolean = step >= FIRST_RECORDING_STEP

    /** What the learner must say at this step, or null for steps 1–2. */
    fun target(state: FixState): String? = when (state.step) {
        3 -> state.word
        4 -> state.chunk
        5 -> state.sentenceText
        else -> null
    }

    /**
     * "Next" on step 1 or 2. Callers must only offer Next on those steps;
     * calling it on a recording step is a programming error.
     */
    fun next(state: FixState): FixState {
        require(!isRecordingStep(state.step)) { "step ${state.step} needs a recording, not Next" }
        return state.copy(step = state.step + 1)
    }

    /**
     * Checks one recording at step 3, 4 or 5. The step passes when the word
     * being fixed is said correctly at its place in the step's target; other
     * words and extra words don't matter. Failing a step
     * [GameConfig.fixFailsBeforeDropBack] times in a row drops back one step.
     *
     * The recording must belong to [state]'s current step: callers should drop
     * a transcript that arrives after the learner moved on.
     */
    fun submit(
        state: FixState,
        heard: String,
        accept: Map<String, Set<String>>,
        config: GameConfig,
    ): FixResult {
        val target = requireNotNull(target(state)) { "step ${state.step} has nothing to record" }
        val heardWords = Normalizer.normalizeHeard(heard)
        if (heardWords.isEmpty()) return FixResult(state, FixOutcome.NOTHING_HEARD, null, 0)

        val alignment = Aligner.align(Normalizer.normalize(target), heardWords, accept)
        val position = when (state.step) {
            3 -> 0
            4 -> state.chunkWordIndex
            else -> state.wordIndex
        }
        require(position < alignment.words.size) { "fix position $position is outside \"$target\"" }
        if (alignment.words[position].status == WordStatus.CORRECT) {
            return if (state.step == STEPS) {
                FixResult(state, FixOutcome.SENTENCE_FIXED, alignment, config.fixStepXp)
            } else {
                FixResult(state.copy(step = state.step + 1, fails = 0), FixOutcome.PASSED, alignment, config.fixStepXp)
            }
        }
        val fails = state.fails + 1
        return if (fails >= config.fixFailsBeforeDropBack) {
            FixResult(state.copy(step = maxOf(1, state.step - 1), fails = 0), FixOutcome.DROPPED_BACK, alignment, 0)
        } else {
            FixResult(state.copy(fails = fails), FixOutcome.TRY_AGAIN, alignment, 0)
        }
    }

    /** The chunk around [wordIndex] and the word's position inside it. */
    private fun chunkAt(sentence: Sentence, words: List<String>, wordIndex: Int): Pair<String, Int> {
        val starts = locateChunks(words, sentence.chunks)
        if (starts != null) {
            sentence.chunks.forEachIndexed { i, chunk ->
                val start = starts[i]
                if (wordIndex >= start && wordIndex < start + Normalizer.normalize(chunk).size) return chunk to wordIndex - start
            }
        }
        val word = words[wordIndex]
        for (chunk in sentence.chunks) {
            val position = Normalizer.normalize(chunk).indexOf(word)
            if (position >= 0) return chunk to position
        }
        return sentence.text to wordIndex
    }

    /**
     * Where each chunk starts among [words], finding the chunks one after
     * another in order, or null if a chunk can't be found. A chunk with no
     * words takes no room.
     */
    private fun locateChunks(words: List<String>, chunks: List<String>): List<Int>? {
        var from = 0
        return chunks.map { chunk ->
            val run = Normalizer.normalize(chunk)
            val start = (from..words.size - run.size).firstOrNull { words.subList(it, it + run.size) == run } ?: return null
            from = start + run.size
            start
        }
    }
}
