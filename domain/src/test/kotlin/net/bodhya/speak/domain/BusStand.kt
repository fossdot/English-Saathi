package net.bodhya.speak.domain

/** The pilot's Bus stand sentences and word data, as test fixtures. */
object BusStand {

    private val words = mapOf(
        "where" to WordInfo(listOf("where"), "वेयर", "v_w"),
        "when" to WordInfo(listOf("when"), "वेन", "v_w"),
        "will" to WordInfo(listOf("will"), "विल", "v_w"),
        "bus" to WordInfo(listOf("bus"), "बस", "short_vowel"),
        "stop" to WordInfo(listOf("stop"), "स्टॉप", "s_cluster"),
        "come" to WordInfo(listOf("come"), "कम", "short_vowel"),
        "much" to WordInfo(listOf("much"), "मच", "short_vowel"),
        "ticket" to WordInfo(listOf("tic", "ket"), "टि-केट", "generic"),
        "please" to WordInfo(listOf("please"), "प्लीज़", "generic"),
        "does" to WordInfo(listOf("does"), "डज़", "generic"),
        "this" to WordInfo(listOf("this"), "दिस", "th"),
        "the" to WordInfo(listOf("the"), "द", "th"),
        "patna" to WordInfo(listOf("Pat", "na"), "पट-ना", "generic", listOf("patna", "patana", "pattna", "patnaa")),
        "bettiah" to WordInfo(
            listOf("Bet", "ti", "ah"), "बे-ति-या", "generic",
            listOf("bettiah", "betia", "bettia", "betiya", "bettiya", "bethia"),
        ),
    )

    private fun sentence(id: String, text: String, hi: String, vararg chunks: String) = Sentence(
        id = id, text = text, hi = hi, chunks = chunks.toList(),
        words = words.filterKeys { it in Normalizer.normalizeHeard(text) },
    )

    val whereIsTheBusStop = sentence("bus_01", "Where is the bus stop?", "बस स्टॉप कहाँ है?", "Where is", "the bus stop")
    val whenWillTheBusCome = sentence("bus_02", "When will the bus come?", "बस कब आएगी?", "When will", "the bus come")
    val oneTicketToPatna = sentence("bus_03", "One ticket to Patna, please.", "पटना का एक टिकट दीजिए।", "One ticket", "to Patna", "please")
    val howMuchIsTheTicket = sentence("bus_04", "How much is the ticket?", "टिकट कितने का है?", "How much", "is the ticket")
    val doesThisBusGoToBettiah = sentence("bus_05", "Does this bus go to Bettiah?", "क्या यह बस बेतिया जाती है?", "Does this bus", "go to Bettiah")

    val lesson = Lesson(
        id = "bus_stand", titleEn = "Bus stand", titleHi = "बस स्टैंड",
        sentences = listOf(whereIsTheBusStop, whenWillTheBusCome, oneTicketToPatna, howMuchIsTheTicket, doesThisBusGoToBettiah),
    )

    val hints = Hints(
        listOf(
            HintTemplate("v_w", "v_w tip", reviewed = false),
            HintTemplate("th", "th tip", reviewed = false),
            HintTemplate("s_cluster", "s_cluster tip", reviewed = false),
            HintTemplate("short_vowel", "short_vowel tip", reviewed = false),
            HintTemplate("generic", "generic tip", reviewed = false),
        ),
    )
}
