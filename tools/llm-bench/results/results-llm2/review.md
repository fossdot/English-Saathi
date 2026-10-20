# LLM outputs for review

Please score each output from 1 (unusable) to 5 (ready to use) in scoring-sheet.csv. Task A: is the Hindi natural for a villager in Bihar, are the chunks sensible, are the sound hints right? Task B: would a slow Hindi reader understand what went wrong and how to fix it?

## Task A: lesson content

### bus_02: When will the bus come?

Reference from the pilot (human): hi = बस कब आएगी?; chunks = ['When will', 'the bus come']

**qwen3:4b-instruct**

```
{
 "text": "When will the bus come?",
 "hi": "बस कहाँ आएगी?",
 "chunks": [
  "When will",
  "the bus come"
 ],
 "words": {
  "when": {
   "syllables": [
    "when"
   ],
   "hint_hi": "वेन",
   "hint_tag": "v_w"
  },
  "will": {
   "syllables": [
    "will"
   ],
   "hint_hi": "विल",
   "hint_tag": "v_w"
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
   "hint_hi": "कोम",
   "hint_tag": "s_cluster"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "When will the bus come?",
 "hi": "बस कब आएगा?",
 "chunks": [
  "When will",
  "the bus come"
 ],
 "words": {
  "when": {
   "syllables": [
    "when"
   ],
   "hint_hi": "बन",
   "hint_tag": "v_w"
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
   "hint_hi": "काम",
   "hint_tag": "s_cluster"
  }
 }
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
```json
{
 "text": "When will the bus come?",
 "hi": "कब बस आयेगा?",
 "chunks": [
  "When will",
 "the bus come"
 ],
 "words": {
  "when": {
```

### bus_03: One ticket to Patna, please.

Reference from the pilot (human): hi = पटना का एक टिकट दीजिए।; chunks = ['One ticket', 'to Patna', 'please']

**qwen3:4b-instruct**

```
{
 "text": "One ticket to Patna, please.",
 "hi": "पटना के लिए एक टिकट, कृपया",
 "chunks": [
  "One ticket",
  "to Patna",
  "please"
 ],
 "words": {
  "one": {
   "syllables": [
    "one"
   ],
   "hint_hi": "ओन",
   "hint_tag": "short_vowel"
  },
  "ticket": {
   "syllables": [
    "tick",
    "et"
   ],
   "hint_hi": "टिकेट",
   "hint_tag": "s_cluster"
  },
  "to": {
   "syllables": [
    "to"
   ],
   "hint_hi": "टो",
   "hint_tag": "th"
  },
  "Patna": {
   "syllables": [
    "Pat",
    "na"
   ],
   "hint_hi": "पटना",
   "hint_tag": "v_w"
  },
  "please": {
   "syllables": [
    "ple",
    "ase"
   ],
   "hint_hi": "प्लीज",
   "hint_tag": "short_vowel"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "One ticket to Patna, please.",
 "hi": "एक टिकट पैदल, कृपया.",
 "chunks": [
  "One",
  "ticket",
  "to",
  "Patna",
  "please."
 ],
 "words": {
  "one": {
   "syllables": [
    "one"
   ],
   "hint_hi": "ए",
   "hint_tag": "th"
  },
  "ticket": {
   "syllables": [
    "ticket"
   ],
   "hint_hi": "टिकट",
   "hint_tag": "v_w"
  },
  "patna": {
   "syllables": [
    "patna"
   ],
   "hint_hi": "पटना",
   "hint_tag": "v_w"
  },
  "please": {
   "syllables": [
    "please"
   ],
   "hint_hi": "प्रे",
   "hint_tag": "th"
  },
  "ticket": {
   "syllables": [
    "ticket"
   ],
   "hint_hi": "टिकट",
   "hint_tag": "v_w"
  },
  "to": {
   "syllables": [
    "to"
   ],
   "hint_hi": "टो",
   "hint_tag": "v_w"
  },
  "Patna": {
   "syllables": [
    "Patna"
   ],
   "hint_hi": "पटना",
   "hint_tag": "v_w"
  },
  "please": {
   "syllables": [
    "please"
   ],
   "hint_hi": "प्रे",
   "hint_tag": "th"
  },
  "ticket": {
   "syllables": [
    "ticket"
   ],
   "hint_hi": "टिकट",
   "hint_tag": "v_w"
  },
  "stop": {
   "syllables": [
    "stop"
   ],
   "hint_hi": "स्टॉप",
   "hint_tag": "v_w"
  }
 }
}
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "One ticket to Patna, please.",
 "hi": "एक टिकट पटना में, कृपया।",
 "chunks": [
  "One ticket",
  "to Patna",
  "please."
 ],
 "words": {
  "one": {
    "syllables": [
      1, "one"
    ],
    "hint_hi": "उन्होंने",
    "hint_tag": "th"
  },
  "ticket": {
    "syllables": [
      "ticket"
    ],
    "hint_hi": "टिकट",
    "hint_tag": "short_vowel"
  },
  "Patna": {
    "syllables": [
      "Patna"
    ],
    "hint_hi": "पटना",
    "hint_tag": "generic"
  }
 }
}
```

### bus_04: How much is the ticket?

Reference from the pilot (human): hi = टिकट कितने का है?; chunks = ['How much', 'is the ticket']

**qwen3:4b-instruct**

```
{
 "text": "How much is the ticket?",
 "hi": "टिकेट कितना है?",
 "chunks": [
  "How much",
  "is the ticket"
 ],
 "words": {
  "much": {
   "syllables": [
    "much"
   ],
   "hint_hi": "मच",
   "hint_tag": "s_cluster"
  },
  "ticket": {
   "syllables": [
    "tick",
    "et"
   ],
   "hint_hi": "टिकेट",
   "hint_tag": "s_cluster"
  },
  "the": {
   "syllables": [
    "the"
   ],
   "hint_hi": "द",
   "hint_tag": "th"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "होम कितना है टिकट?",
 "hi": "होम कितना है टिकट?",
 "chunks": [
  "होम कितना है",
  "टिकट"
 ],
 "words": {
  "how": {
   "syllables": [
    "होम कितना है"
   ],
   "hint_hi": "होम कितना है",
   "hint_tag": "v_w"
  },
  "much": {
   "syllables": [
    "होम कितना है"
   ],
   "hint_hi": "होम कितना है",
   "hint_tag": "v_w"
  },
  "is": {
   "syllables": [
    "होम कितना है"
   ],
   "hint_hi": "होम कितना है",
   "hint_tag": "v_w"
  },
  "ticket": {
   "syllables": [
    "होम कितना है"
   ],
   "hint_hi": "होम कितना है",
   "hint_tag": "v_w"
  }
 }
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
```json
{
 "text": "How much is the ticket?",
 "hi": "कितना है टिकट?",
 "chunks": [
  "How much",
  "is the ticket"
 ],
 "words": {
  "how": {
    "syllables": [
```

### bus_05: Does this bus go to Bettiah?

Reference from the pilot (human): hi = क्या यह बस बेतिया जाती है?; chunks = ['Does this bus', 'go to Bettiah']

**qwen3:4b-instruct**

```
{
 "text": "Does this bus go to Bettiah?",
 "hi": "बेटिया जाने वाला यह बस क्या है?",
 "chunks": [
  "Does this bus",
  "go to Bettiah"
 ],
 "words": {
  "does": {
   "syllables": [
    "does"
   ],
   "hint_hi": "डेस",
   "hint_tag": "v_w"
  },
  "this": {
   "syllables": [
    "this"
   ],
   "hint_hi": "टिस",
   "hint_tag": "s_cluster"
  },
  "bus": {
   "syllables": [
    "bus"
   ],
   "hint_hi": "बस",
   "hint_tag": "short_vowel"
  },
  "go": {
   "syllables": [
    "go"
   ],
   "hint_hi": "गो",
   "hint_tag": "th"
  },
  "Bettiah": {
   "syllables": [
    "Bettiah"
   ],
   "hint_hi": "बेटिया",
   "hint_tag": "generic"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "Does this bus go to Bettiah?",
 "hi": "क्या यह बस बेतिया जाता है?",
 "chunks": [
  "Does",
  "this",
  "bus",
  "go",
  "to",
  "Bettiah"
 ],
 "words": {
  "does": {
   "syllables": [
    "does"
   ],
   "hint_hi": "क्या",
   "hint_tag": "v_w"
  },
  "this": {
   "syllables": [
    "this"
   ],
   "hint_hi": "क्या",
   "hint_tag": "th"
  },
  "bus": {
   "syllables": [
    "bus"
   ],
   "hint_hi": "बस",
   "hint_tag": "short_vowel"
  },
  "go": {
   "syllables": [
    "go"
   ],
   "hint_hi": "जाता है"
  }
 }
}
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
```json
{
 "text": "Does this bus go to Bettiah?",
 "hi": "इस बस बेतिहा जाती है?",
 "chunks": [
  "Does this",
  "bus go",
  "to Bettiah"
 ],
 "words": {
  "does": {
```

### market_01: How much is one kilo of rice?

**qwen3:4b-instruct**

```
{
 "text": "How much is one kilo of rice?",
 "hi": "रिस के एक किलो कितना है?",
 "chunks": [
  "How much is",
  "one kilo of rice"
 ],
 "words": {
  "much": {
   "syllables": [
    "much"
   ],
   "hint_hi": "मच",
   "hint_tag": "v_w"
  },
  "one": {
   "syllables": [
    "one"
   ],
   "hint_hi": "ओन",
   "hint_tag": "short_vowel"
  },
  "kilo": {
   "syllables": [
    "ki",
    "lo"
   ],
   "hint_hi": "किलो",
   "hint_tag": "s_cluster"
  },
  "rice": {
   "syllables": [
    "rice"
   ],
   "hint_hi": "रिस",
   "hint_tag": "short_vowel"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "How much is one kilo of rice?",
 "hi": "एक किलोग्राम चावल कितना है?",
 "chunks": [
  "How much",
  "is one kilo",
  "of rice"
 ],
 "words": {
  "how": {
   "syllables": [
    "how"
   ],
   "hint_hi": "हेर",
   "hint_tag": "v_w"
  },
  "much": {
   "syllables": [
    "much"
   ],
   "hint_hi": "मूक",
   "hint_tag": "v_w"
  },
  "is": {
   "syllables": [
    "is"
   ],
   "hint_hi": "एज़",
   "hint_tag": "v_w"
  },
  "one": {
   "syllables": [
    "one"
   ],
   "hint_hi": "एज़",
   "hint_tag": "v_w"
  },
  "kilogram": {
   "syllables": [
    "kilogram"
   ],
   "hint_hi": "किलोग्राम",
   "hint_tag": "v_w"
  }
 }
}
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
```json
{
 "text": "How much is one kilo of rice?",
 "hi": "एक किलो आफ राइस का मूल्य कितना है?",
 "chunks": [
  "How much",
  "is one kilo",
  "of rice"
 ],
 "words": {
  "how": {
    "syllables": [
```

### market_02: Please give me two kilos of onions.

**qwen3:4b-instruct**

```
{
 "text": "Please give me two kilos of onions.",
 "hi": "कृपया मुझे दो किलो आनंद दें.",
 "chunks": [
  "Please give me",
  "two kilos of onions"
 ],
 "words": {
  "please": {
   "syllables": [
    "please"
   ],
   "hint_hi": "प्लीज",
   "hint_tag": "v_w"
  },
  "two": {
   "syllables": [
    "two"
   ],
   "hint_hi": "टू",
   "hint_tag": "short_vowel"
  },
  "kilos": {
   "syllables": [
    "ki",
    "los"
   ],
   "hint_hi": "किलो",
   "hint_tag": "s_cluster"
  },
  "onions": {
   "syllables": [
    "on",
    "ions"
   ],
   "hint_hi": "ओनियन",
   "hint_tag": "s_cluster"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "Please give me two kilos of onions?",
 "hi": "कृपया मुझे दो किलो आलू दें?",
 "chunks": [
  "Please",
  "give",
  "me",
  "two",
  "kilos",
  "of",
  "onions"
 ],
 "words": {
  "please": {
   "syllables": [
    "please"
   ],
   "hint_hi": "कृपया",
   "hint_tag": "v_w"
  },
  "give": {
   "syllables": [
    "give"
   ],
   "hint_hi": "कृपया",
   "hint_tag": "v_w"
  },
  "me": {
   "syllables": [
    "me"
   ],
   "hint_hi": "मे",
   "hint_tag": "v_w"
  },
  "two": {
   "syllables": [
    "two"
   ],
   "hint_hi": "दो",
   "hint_tag": "v_w"
  },
  "kilos": {
   "syllables": [
    "kilos"
   ],
   "hint_hi": "किलोस",
   "hint_tag": "v_w"
  },
  "onions": {
   "syllables": [
    "onions"
   ],
   "hint_hi": "आलू",
   "hint_tag": "v_w"
  }
 }
}
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
```json
{
 "text": "Please give me two kilos of onions.",
 "hi": "प्लीज दें मुझे दो किलो आलू",
 "chunks": [
  "Please give me",
  "two kilos of",
  "onions."
 ],
 "words": {
  "please": {
    "syllables": [
      please
    ],
    "hint_hi": "प्लीज",
    "hint_tag": "short_vowel"
  },
  "two": {
    "syllables": [
      two
    ],
    "hint_hi": "दो",
    "hint_tag": "short_vowel"
  },
  "kilos": {
    "syllables": [
      kilo,
      s
    ],
    "hint_hi": "किलो",
    "hint_tag": "short_vowel"
  },
  "onions": {
    "syllables": [
      onions
    ],
    "hint_hi": "आलू",
    "hint_tag": "generic"
  }
 }
}
```
```

### bank_01: I want to open a bank account.

**qwen3:4b-instruct**

```
{
 "text": "I want to open a bank account.",
 "hi": "मैं एक बैंक अकाउंट खोलना चाहता हूँ",
 "chunks": [
  "I want to",
  "open a bank account"
 ],
 "words": {
  "want": {
   "syllables": [
    "want"
   ],
   "hint_hi": "वैंट",
   "hint_tag": "v_w"
  },
  "open": {
   "syllables": [
    "open"
   ],
   "hint_hi": "ओपन",
   "hint_tag": "s_cluster"
  },
  "bank": {
   "syllables": [
    "bank"
   ],
   "hint_hi": "बैंक",
   "hint_tag": "short_vowel"
  },
  "account": {
   "syllables": [
    "ac",
    "count"
   ],
   "hint_hi": "एकाउंट",
   "hint_tag": "s_cluster"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "I want to open a bank account?",
 "hi": "आई वाउड ओपन ए बैंक अकाउंट?",
 "chunks": [
  "I want",
  "to open",
  "a bank account"
 ],
 "words": {
  "want": {
   "syllables": [
    "want"
   ],
   "hint_hi": "वाउड",
   "hint_tag": "v_w"
  },
  "to": {
   "syllables": [
    "to"
   ],
   "hint_hi": "टू",
   "hint_tag": "th"
  },
  "open": {
   "syllables": [
    "open"
   ],
   "hint_hi": "ओपन",
   "hint_tag": "short_vowel"
  },
  "account": {
   "syllables": [
    "account"
   ],
   "hint_hi": "एकाउंट",
   "hint_tag": "s_cluster"
  }
 }
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
```json
{
 "text": "I want to open a bank account.",
 "hi": "मैं चाहता हूँ कि मैं बैंक अकाउंट खोलूँ",
 "chunks": [
  "I want",
  "to open",
  "a bank account"
 ],
 "words": {
  "want": {
```

### bank_02: Where do I sign?

**qwen3:4b-instruct**

```
{
 "text": "Where do I sign?",
 "hi": "बस स्टॉप कहाँ है?",
 "chunks": [
  "Where do",
  "I sign"
 ],
 "words": {
  "where": {
   "syllables": [
    "where"
   ],
   "hint_hi": "वेयर",
   "hint_tag": "v_w"
  },
  "do": {
   "syllables": [
    "do"
   ],
   "hint_hi": "डो",
   "hint_tag": "th"
  },
  "sign": {
   "syllables": [
    "sign"
   ],
   "hint_hi": "साइन",
   "hint_tag": "s_cluster"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "Where do I sign?",
 "hi": "वेयर डू इज सिग्न?",
 "chunks": [
  "Where do I",
  "sign"
 ],
 "words": {
  "where": {
   "syllables": [
    "where"
   ],
   "hint_hi": "वेयर",
   "hint_tag": "v_w"
  },
  "do": {
   "syllables": [
    "do"
   ],
   "hint_hi": "डू",
   "hint_tag": "short_vowel"
  },
  "sign": {
   "syllables": [
    "sign"
   ],
   "hint_hi": "सिग्न",
   "hint_tag": "s_cluster"
  }
 }
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
```json
{
 "text": "Where do I sign?",
 "hi": "कहाँ मैं साइन करता हूँ?",
 "chunks": [
  "Where do I",
  "sign?"
 ],
 "words": {
  "where": {
   "syllables": [
    "where"
  ],
   "hint_hi": "वेयर",
   "hint_tag": "v_w"
 },
 "do": {
  "syllables": [
   "do"
  ],
  "hint_hi": "दो",
  "hint_tag": "short_vowel"
 },
 "I": {
  "syllables": [
   "I"
  ],
  "hint_hi": "मैं",
  "hint_tag": "generic"
 },
 "sign": {
  "syllables": [
   "sign"
  ],
  "hint_hi": "साइन",
  "hint_tag": "generic"
 }
}
}
```
```

### hospital_01: My child has had a fever since yesterday.

**qwen3:4b-instruct**

```
{
 "text": "My child has had a fever since yesterday.",
 "hi": "मेरा बच्चा पिछले दिन से बीमार है",
 "chunks": [
  "My child has had",
  "a fever since yesterday"
 ],
 "words": {
  "child": {
   "syllables": [
    "child"
   ],
   "hint_hi": "चिल्ड",
   "hint_tag": "s_cluster"
  },
  "has": {
   "syllables": [
    "has"
   ],
   "hint_hi": "है",
   "hint_tag": "short_vowel"
  },
  "fever": {
   "syllables": [
    "fever"
   ],
   "hint_hi": "फीवर",
   "hint_tag": "v_w"
  },
  "since": {
   "syllables": [
    "since"
   ],
   "hint_hi": "सीन",
   "hint_tag": "v_w"
  },
  "yesterday": {
   "syllables": [
    "yesterday"
   ],
   "hint_hi": "कल",
   "hint_tag": "generic"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "My child has had a fever since yesterday.",
 "hi": "मेरा बच्चा कल से बुखार है.",
 "chunks": [
  "My",
  "child",
  "has",
  "had",
  "a",
  "fever",
  "since",
  "yesterday."
 ],
 "words": {
  "my": {
   "syllables": [
    "my"
   ],
   "hint_hi": "मेरा",
   "hint_tag": "v_w"
  },
  "child": {
   "syllables": [
    "child"
   ],
   "hint_hi": "बच्चा",
   "hint_tag": "v_w"
  },
  "has": {
   "syllables": [
    "has"
   ],
   "hint_hi": "है",
   "hint_tag": "v_w"
  },
  "a": {
   "syllables": [
    "a"
   ],
   "hint_hi": "ए",
   "hint_tag": "v_w"
  },
  "fever": {
   "syllables": [
    "fever"
   ],
   "hint_hi": "बुखार",
   "hint_tag": "v_w"
  },
  "since": {
   "syllables": [
    "since"
   ],
   "hint_hi": "से",
   "hint_tag": "v_w"
  },
  "yesterday": {
   "syllables": [
    "yesterday"
   ],
   "hint_hi": "कल",
   "hint_tag": "v_w"
  }
 }
}
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
```json
{
 "text": "My child has had a fever since yesterday.",
 "hi": "मेरे बच्चे का तबीयत बढ़ गया है ये बचपन से।",
 "chunks": [
  "My child",
  "has had a fever",
  "since yesterday"
 ],
 "words": {
  "child": {
    "syllables": [
```

### hospital_02: Which doctor should I see?

**qwen3:4b-instruct**

```
{
 "text": "Which doctor should I see?",
 "hi": "किस डॉक्टर को देखना चाहिए?",
 "chunks": [
  "Which doctor",
  "should I see"
 ],
 "words": {
  "which": {
   "syllables": [
    "which"
   ],
   "hint_hi": "विच",
   "hint_tag": "v_w"
  },
  "doctor": {
   "syllables": [
    "doc",
    "tor"
   ],
   "hint_hi": "डॉक्टर",
   "hint_tag": "s_cluster"
  },
  "should": {
   "syllables": [
    "sh",
    "ould"
   ],
   "hint_hi": "शाऊल्ड",
   "hint_tag": "th"
  },
  "see": {
   "syllables": [
    "see"
   ],
   "hint_hi": "सी",
   "hint_tag": "short_vowel"
  }
 }
}
```

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "Which doctor should I see?",
 "hi": "डॉक्टर कौन देखूँ?",
 "chunks": [
  "Which",
  "doctor",
  "should",
  "I",
  "see"
 ],
 "words": {
  "which": {
   "syllables": [
    "which"
   ],
   "hint_hi": "डॉक्टर",
   "hint_tag": "v_w"
  },
  "doctor": {
   "syllables": [
    "doctor"
   ],
   "hint_hi": "डॉक्टर",
   "hint_tag": "v_w"
  },
  "should": {
   "syllables": [
    "should"
   ],
   "hint_hi": "डॉक्टर",
   "hint_tag": "v_w"
  },
  "I": {
   "syllables": [
    "I"
   ],
   "hint_hi": "डॉक्टर",
   "hint_tag": "v_w"
  },
  "see": {
   "syllables": [
    "see"
   ],
   "hint_hi": "डॉक्टर",
   "hint_tag": "v_w"
  }
 }
}
```

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**

```
```json
{
 "text": "Which doctor should I see?",
 "hi": "कौन सा डॉक्टर देखना है?",
 "chunks": [
  "Which doctor",
  "should I see"
 ],
 "words": {
  "which": {
```

## Task B: explaining the first mistake

### bus_01-1

- Sentence: Where is the bus stop? (बस स्टॉप कहाँ है?)
- Heard: Where is bus stop?
- First mistake: {'index': 2, 'word': 'the', 'status': 'MISSING', 'heard_as': None}

**qwen3:4b-instruct**: आपने "the" कहा नहीं, इसलिए गलत सुनाया। "the" कहिए, जैसे "the bus stop"।

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**: सेंटेंस: "बस स्टॉप कहाँ है?"
मीनिंग इन हिंदी: बस स्टॉप कहाँ है?
रोकने वाला सिस्टम ने सुना: "बस स्टॉप कहाँ है?"
मिसिंग वर्ड: "द" (वर्ड 3 of 5) नहीं था।

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**: आपने बस सटप कहाँ है? बस सटप के बारे में पूछते समय "the" शब्द भूल गए। सही तरीके से बोलना है: "बस सटप कहाँ है?"

### bus_01-2

- Sentence: Where is the bus stop? (बस स्टॉप कहाँ है?)
- Heard: Where is the boss stop?
- First mistake: {'index': 3, 'word': 'bus', 'status': 'WRONG', 'heard_as': 'boss'}

**qwen3:4b-instruct**: आपने "bus" कहा "boss" जैसा, इसलिए गलत हुआ। आप "bus" कहिए, जैसे "बस"।

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**: सेंटेंस: "बस स्टॉप कहाँ है?"
मीनिंग हिन्‍दी में: बस स्टॉप कहाँ है?
रोकने वाला सिर्फ़ हिन्‍दी के शब्दों को समझता है। यहाँ पर कोई अंग्रेज़ी शब्द नहीं है। यहाँ केवल हिन्‍दी के शब्दों को समझा गया है। यहाँ पर कोई अंग्रेज़ी शब्द नहीं है।

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**: बस का सही शब्द "बस" है, नहीं "बॉस". आपने "बस" को "बॉस" में बदल दिया।

### bus_01-3

- Sentence: Where is the bus stop? (बस स्टॉप कहाँ है?)
- Heard: Where is the bus shop?
- First mistake: {'index': 4, 'word': 'stop', 'status': 'WRONG', 'heard_as': 'shop'}

**qwen3:4b-instruct**: आपने "stop" कहा "shop" जैसा, लेकिन सही तरीके से कहना चाहिए "stop"।

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**: सेंटेंस: "बस स्टॉप कहाँ है?"
मीनिंग हिन्‍दी में: बस स्टॉप कहाँ है?
रोकने वाला सिस्टम ने सुना: "बस स्टॉप कहाँ है?"
मिसफायर: "स्टॉप" शब्द (5वां शब्द 5वां) को "शॉप" कहा गया।

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**: शब्द "स्टॉप" (पांचवां शब्द) सही तरीके से नहीं बोला गया। सही तरीके से बोलना है "स्टॉप"। आप इसे इस प्रकार कह सकते हैं: "बस स्टॉप कहाँ है?"

### bus_02-4

- Sentence: When will the bus come? (बस कब आएगी?)
- Heard: When the bus will come?
- First mistake: {'index': 1, 'word': 'will', 'status': 'MISSING', 'heard_as': None}

**qwen3:4b-instruct**: आपने "will" कहा नहीं। इसे कहें जैसे "will"।

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**: बस कब आएगी?

शब्दों की पहचान:
बस कब आएगी?

शब्दों की पहचान:
बस कब आएगी?

शब्दों की पहचान:
बस कब आएगी?

शब्दों की पहचान:
बस कब आएगी?

शब्दों की पहचान:
बस कब आएगी?

शब्दों की पहचान:
बस कब आएगी?

शब्दों की पहचान:
बस कब आएगी?

शब्दों की पहचान:
बस कब आएगी?

शब्दों की पहचान:
बस कब आएगी?

शब्दों की पहचान:

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**: आपने पूछा है, "बस कब आएगा?" लेकिन आपने "will" शब्द को भूल कर दिया. इसे सही तरीके से बोलना है: "बस कब आएगा?"

### bus_03-5

- Sentence: One ticket to Patna, please. (पटना का एक टिकट दीजिए।)
- Heard: Van ticket to Patna, please.
- First mistake: {'index': 0, 'word': 'one', 'status': 'WRONG', 'heard_as': 'van'}

**qwen3:4b-instruct**: आपने "एक" कहा था जो "वैन" है। आप "एक" कहिए, जैसे "एक टिकट"।

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**: पटना का एक टिकट दीजिए।
The recognizer heard: "Van ticket to Patna, please."
First mistake: the word "one" (word 1 of 5) was said as "van".

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**: आपने "एक" को "वान" बोल दिया. सही शब्द "एक" है. "एक" को "ek" के रूप में सीखो और याद रखो.

### bus_04-6

- Sentence: How much is the ticket? (टिकट कितने का है?)
- Heard: How much the ticket?
- First mistake: {'index': 2, 'word': 'is', 'status': 'MISSING', 'heard_as': None}

**qwen3:4b-instruct**: आपने "is" कहा नहीं, इसलिए गलत है। आप कहें चाहिए, "How much is the ticket?"

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**: टिकट कितने का है?

The recognizer heard: "How much the ticket?"

First mistake: the word "is" (word 3 of 5) was missing.

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**: आपने टिकट कितना है? "टिकट कितना है?" ये शब्द "है" (is) को भूल गए।

### bus_05-7

- Sentence: Does this bus go to Bettiah? (क्या यह बस बेतिया जाती है?)
- Heard: Does this bus goes to Bettiah?
- First mistake: {'index': 3, 'word': 'go', 'status': 'WRONG', 'heard_as': 'goes'}

**qwen3:4b-instruct**: आपने "go" कहा "goes" के जैसा, लेकिन सही तरीके से कहें "go"।

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**: क्या यह बस बेतिया जाती है?
The recognizer heard: "Does this bus goes to Bettiah?"
First mistake: the word "go" (word 4 of 6) was said as "goes".

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**: आपने "go" शब्द को "goes" के बजाय "जाऊँ" (jaun) ही सुना। "Go" का सरल रूप "जाओ" (jau) है।

### bus_05-8

- Sentence: Does this bus go to Bettiah? (क्या यह बस बेतिया जाती है?)
- Heard: Do this bus go to Bettiah?
- First mistake: {'index': 0, 'word': 'does', 'status': 'WRONG', 'heard_as': 'do'}

**qwen3:4b-instruct**: आपने "does" कहा था जैसे "do"। इसके लिए ठीक तरीके से कहें "does"।

**hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M**: क्या यह बस बेतिया जाती है?

The recognizer heard: "Do this bus go to Bettiah?"

First mistake: the word "does" (word 1 of 6) was said as "do".

**hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M**: आपने "does" को "do" ही बोल दिया. सही शब्द "does" है. "Does" को बोलने का तरीका याद करें.
