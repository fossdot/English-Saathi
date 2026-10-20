# LLM outputs for review

Please score each output from 1 (unusable) to 5 (ready to use) in scoring-sheet.csv. Task A: is the Hindi natural for a villager in Bihar, are the chunks sensible, are the sound hints right? Task B: would a slow Hindi reader understand what went wrong and how to fix it?

## Task A: lesson content

### bus_02: When will the bus come?

Reference from the pilot (human): hi = बस कब आएगी?; chunks = ['When will', 'the bus come']

**llama3.2:1b**

```
{
 "text": "When will the bus come?",
 "hi": "कल क्या होगा?",
 "chunks": [
  "When",
  "will",
  "the",
  "bus",
  "come"
 ],
 "words": {
  "when": {
   "syllables": [
    "when"
   ],
   "hint_hi": "कल",
   "hint_tag": "v_w"
  },
  "will": {
   "syllables": [
    "will"
   ],
   "hint_hi": "क्या",
   "hint_tag": "th"
  },
  "the": {
   "syllables": [
    "the"
   ],
   "hint_hi": "द",
   "hint_tag": "th"
  },
  "bus": {
   "syllables": [
    "bus"
   ],
   "hint_hi": "बस",
   "hint_tag": "short_vowel"
  },
  "come": {
   "syllables": [
    "come"
   ],
   "hint_hi": "कम",
   "hint_tag": "s_cluster"
  }
 }
```

## Task B: explaining the first mistake

### bus_01-1

- Sentence: Where is the bus stop? (बस स्टॉप कहाँ है?)
- Heard: Where is bus stop?
- First mistake: {'index': 2, 'word': 'the', 'status': 'MISSING', 'heard_as': None}

**llama3.2:1b**: आपके शब्दों में एक शब्द की कमी है। आप इस शब्द को "the" कहकर पूरा कर सकते हैं।
