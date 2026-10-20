# LLM outputs for review

Please score each output from 1 (unusable) to 5 (ready to use) in scoring-sheet.csv. Task A: is the Hindi natural for a villager in Bihar, are the chunks sensible, are the sound hints right? Task B: would a slow Hindi reader understand what went wrong and how to fix it?

## Task A: lesson content

### bus_02: When will the bus come?

Reference from the pilot (human): hi = बस कब आएगी?; chunks = ['When will', 'the bus come']

**gemma3:4b**

```
```json
{
 "text": "When will the bus come?",
 "hi": "कब बस आएगी?",
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
   "hint_hi": "कम",
   "hint_tag": "short_vowel"
  }
 }
}
```
```

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "बस कब आएगी?",
  "hi": "बस कब आएगी?",
  "chunks": [
    "When will",
    "the bus come"
  ],
  "words": {
    "when": {
      "syllables": ["when"],
      "hint_hi": "वेन",
      "hint_tag": "generic"
    },
    "the": {
      "syllables": ["the"],
      "hint_hi": "द",
      "hint_tag": "generic"
    },
    "bus": {
      "syllables": ["bus"],
      "hint_hi": "बस",
      "hint_tag": "generic"
    },
    "come": {
      "syllables": ["come"],
      "hint_hi": "आना",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "When will the bus come?",
 "hi": "बस कब आएगी?",
 "chunks": [
  "When",
  "will",
  "the",
  "bus",
  "come",
  "will",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
  "the",
  "bus",
  "come",
```

### bus_03: One ticket to Patna, please.

Reference from the pilot (human): hi = पटना का एक टिकट दीजिए।; chunks = ['One ticket', 'to Patna', 'please']

**gemma3:4b**

```
```json
{
 "text": "One ticket to Patna, please.",
 "hi": "एक टिकट पटना के लिए, कृपया।",
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
   "hint_hi": "वन",
   "hint_tag": "generic"
  },
  "ticket": {
   "syllables": [
    "tick-et"
   ],
   "hint_hi": "टिकट",
   "hint_tag": "s_cluster"
  },
  "to": {
   "syllables": [
    "to"
   ],
   "hint_hi": "को",
   "hint_tag": "generic"
  },
  "patna": {
   "syllables": [
    "pat-na"
   ],
   "hint_hi": "पटना",
   "hint_tag": "generic"
  },
  "please": {
   "syllables": [
    "please"
   ],
   "hint_hi": "कृपया",
   "hint_tag": "generic"
  }
 }
}
```
```

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "एक टिकट पटना, कृपया",
  "hi": "एक टिकट पटना, कृपया",
  "chunks": [
    "One ticket to Patna",
    "please"
  ],
  "words": {
    "ticket": {
      "syllables": ["टिकट"],
      "hint_hi": "टिकट",
      "hint_tag": "generic"
    },
    "to": {
      "syllables": ["to"],
      "hint_hi": "से",
      "hint_tag": "generic"
    },
    "patna": {
      "syllables": ["पटना"],
      "hint_hi": "पटना",
      "hint_tag": "generic"
    },
    "please": {
      "syllables": ["कृपया"],
      "hint_hi": "कृपया",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "One ticket to Patna, please.",
 "hi": "एक टिकट पटना, कृपया.",
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
   "hint_hi": "एक",
   "hint_tag": "v_w"
  },
  "ticket": {
   "syllables": [
    "ticket"
   ],
   "hint_hi": "टिकट",
   "hint_tag": "th"
  },
  "to": {
   "syllables": [
    "to",
    "Patna"
   ],
   "hint_hi": "पटना",
   "hint_tag": "s_cluster"
  },
  "please": {
   "syllables": [
    "please."
   ],
   "hint_hi": "कृपया.",
   "hint_tag": "v_w"
  }
 }
}
```

### bus_04: How much is the ticket?

Reference from the pilot (human): hi = टिकट कितने का है?; chunks = ['How much', 'is the ticket']

**gemma3:4b**

```
```json
{
 "text": "How much is the ticket?",
 "hi": "टिकट का मूल्य कितना है?",
 "chunks": [
  "How much",
  "is the",
  "ticket?"
 ],
 "words": {
  "how": {
   "syllables": [
    "how"
   ],
   "hint_hi": "हाउ",
   "hint_tag": "generic"
  },
  "much": {
   "syllables": [
    "much"
   ],
   "hint_hi": "मच",
   "hint_tag": "generic"
  },
  "is": {
   "syllables": [
    "is"
   ],
   "hint_hi": "इज़",
   "hint_tag": "th"
  },
  "the": {
   "syllables": [
    "the"
   ],
   "hint_hi": "द",
   "hint_tag": "th"
  },
  "ticket": {
   "syllables": [
    "tick-et"
   ],
   "hint_hi": "टिकट",
   "hint_tag": "s_cluster"
  }
 }
}
```
```

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "How much is the ticket?",
  "hi": "टिकट का कितना पैसा है?",
  "chunks": [
    "How much",
    "is the ticket"
  ],
  "words": {
    "much": {
      "syllables": ["much"],
      "hint_hi": "बहुत",
      "hint_tag": "generic"
    },
    "ticket": {
      "syllables": ["ticket"],
      "hint_hi": "टिकट",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "How much is the ticket?",
 "hi": "कैसे का टिकट?",
 "chunks": [
  "How",
  "much",
  "is",
  "the",
  "ticket"
 ],
 "words": {
  "how": {
   "syllables": [
    "How"
   ],
   "hint_hi": "कैसे",
   "hint_tag": "v_w"
  },
  "much": {
   "syllables": [
    "much"
   ],
   "hint_hi": "कैसे",
   "hint_tag": "th"
  },
  "is": {
   "syllables": [
    "is"
   ],
   "hint_hi": "कैसे",
   "hint_tag": "s_cluster"
  },
  "ticket": {
   "syllables": [
    "ticket"
   ],
   "hint_hi": "टिकट",
   "hint_tag": "s_cluster"
  }
 }
}
```

### bus_05: Does this bus go to Bettiah?

Reference from the pilot (human): hi = क्या यह बस बेतिया जाती है?; chunks = ['Does this bus', 'go to Bettiah']

**gemma3:4b**

```
```json
{
 "text": "Does this bus go to Bettiah?",
 "hi": "क्या यह बस बेटियाह जाती है?",
 "chunks": [
  "Does this",
  "bus go",
  "to Bettiah"
 ],
 "words": {
  "does": {
   "syllables": [
    "does"
   ],
   "hint_hi": "डू",
   "hint_tag": "generic"
  },
  "this": {
   "syllables": [
    "this"
   ],
   "hint_hi": "यह",
   "hint_tag": "generic"
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
   "hint_hi": "जाओ",
   "hint_tag": "generic"
  },
  "to": {
   "syllables": [
    "to"
   ],
   "hint_hi": "को",
   "hint_tag": "generic"
  },
  "bettiah": {
   "syllables": [
    "bet-tiah"
   ],
   "hint_hi": "बेटियाह",
   "hint_tag": "s_cluster"
  }
 }
}
```
```

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "क्या यह बस बेतिया जाती है?",
  "hi": "क्या यह बस बेतिया जाती है?",
  "chunks": [
    "Does this bus",
    "go to Bettiah"
  ],
  "words": {
    "Does": {
      "syllables": ["does"],
      "hint_hi": "क्या",
      "hint_tag": "generic"
    },
    "this": {
      "syllables": ["this"],
      "hint_hi": "यह",
      "hint_tag": "generic"
    },
    "bus": {
      "syllables": ["bus"],
      "hint_hi": "बस",
      "hint_tag": "generic"
    },
    "go": {
      "syllables": ["go"],
      "hint_hi": "जाना",
      "hint_tag": "generic"
    },
    "to": {
      "syllables": ["to"],
      "hint_hi": "को",
      "hint_tag": "generic"
    },
    "Bettiah": {
      "syllables": ["Bettiah"],
      "hint_hi": "बेतिया",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "Does this bus go to Bettiah?",
 "hi": "क्या यह बस Bettiah जाती है?",
 "chunks": [
  "Does",
  "this bus",
  "go",
  "to",
  "Bettiah"
 ],
 "words": {
  "does": {
    "hint_hi": "क्या",
    "hint_tag": "v_w",
    "go": {
      "hint_hi": "जाती",
      "hint_tag": "th"
    },
    "this": {
      "hint_hi": "यह",
      "hint_tag": "s_cluster",
    "bus": {
      "hint_hi": "बस",
      "hint_tag": "s_cluster",
      "go": {
        "hint_hi": "जाती",
        "hint_tag": "th"
      },
      "to": {
        "hint_hi": "Bettiah",
        "hint_tag": "s_cluster"
      }
    }
  },
  "Bettiah": {
    "hint_hi": "बेटा",
    "hint_tag": "s_cluster"
  }
 }
}
```

### market_01: How much is one kilo of rice?

**gemma3:4b**

```
```json
{
 "text": "How much is one kilo of rice?",
 "hi": "एक किलो चावल का मूल्य कितना है?",
 "chunks": [
  "How much",
  "is one",
  "kilo of rice"
 ],
 "words": {
  "how": {
   "syllables": [
    "how"
   ],
   "hint_hi": "हाउ",
   "hint_tag": "generic"
  },
  "much": {
   "syllables": [
    "much"
   ],
   "hint_hi": "मच",
   "hint_tag": "generic"
  },
  "is": {
   "syllables": [
    "is"
   ],
   "hint_hi": "इज़",
   "hint_tag": "v_w"
  },
  "one": {
   "syllables": [
    "one"
   ],
   "hint_hi": "वन",
   "hint_tag": "short_vowel"
  },
  "kilo": {
   "syllables": [
    "ki-lo",
    "kilo"
   ],
   "hint_hi": "किलो",
   "hint_tag": "generic"
  },
  "of": {
   "syllables": [
    "of"
   ],
   "hint_hi": "ऑफ़",
   "hint_tag": "generic"
  },
  "rice": {
   "syllables": [
    "rice"
   ],
   "hint_hi": "चावल",
   "hint_tag": "generic"
  }
 }
}
```
```

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "How much is one kilo of rice?",
  "hi": "एक किलो चावल का कितना है?",
  "chunks": [
    "How much is",
    "one kilo of rice"
  ],
  "words": {
    "one": {
      "syllables": ["one"],
      "hint_hi": "एक",
      "hint_tag": "generic"
    },
    "kilo": {
      "syllables": ["kilo"],
      "hint_hi": "किलो",
      "hint_tag": "generic"
    },
    "rice": {
      "syllables": ["rice"],
      "hint_hi": "चावल",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "How much is one kilo of rice?",
 "hi": "किलो किलोग्राम चावल कितना है?",
 "chunks": [
  "How much",
  "is one kilo of",
  "rice"
 ],
 "words": {
  "one": {
   "syllables": [
    "one"
   ],
   "hint_hi": "एक",
   "hint_tag": "v_w"
  },
  "kilo": {
   "syllables": [
    "kilo"
   ],
   "hint_hi": "किलो",
   "hint_tag": "th"
  },
  "of": {
   "syllables": [
    "of"
   ],
   "hint_hi": "चा",
   "hint_tag": "s_cluster"
  },
  "rice": {
   "syllables": [
    "चा"
   ],
   "hint_hi": "चा",
   "hint_tag": "s_cluster"
  }
 },
 "words": {
  "rice": {
   "syllables": [
    "चा"
   ],
   "hint_hi": "चा",
   "hint_tag": "s_cluster"
  }
 }
}
```

### market_02: Please give me two kilos of onions.

**gemma3:4b**

```
```json
{
 "text": "Please give me two kilos of onions.",
 "hi": "कृपया मुझे दो किलो प्याज दो।",
 "chunks": [
  "Please give",
  "me two kilos",
  "of onions"
 ],
 "words": {
  "please": {
   "syllables": [
    "plea-se"
   ],
   "hint_hi": "प्लीज़",
   "hint_tag": "generic"
  },
  "give": {
   "syllables": [
    "give"
   ],
   "hint_hi": "गिव",
   "hint_tag": "generic"
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
    "ki-los"
   ],
   "hint_hi": "किलो",
   "hint_tag": "generic"
  },
  "of": {
   "syllables": [
    "of"
   ],
   "hint_hi": "ऑफ",
   "hint_tag": "generic"
  },
  "onions": {
   "syllables": [
    "o-nyons"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  }
 }
}
```
```

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "कृपया मुझे दो किलो प्याज दें",
  "hi": "कृपया मुझे दो किलो प्याज दीजिए",
  "chunks": [
    "Please give me",
    "two kilos of onions"
  ],
  "words": {
    "give": {
      "syllables": ["give"],
      "hint_hi": "दीजिए",
      "hint_tag": "generic"
    },
    "two": {
      "syllables": ["two"],
      "hint_hi": "दो",
      "hint_tag": "generic"
    },
    "kilos": {
      "syllables": ["kilos"],
      "hint_hi": "किलो",
      "hint_tag": "generic"
    },
    "onions": {
      "syllables": ["onions"],
      "hint_hi": "प्याज",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "Please give me two kilos of onions.",
 "hi": "कृपया मुझे दो किलो प्याज दो.",
 "chunks": [
  "Please give me",
  "two kilos of onions."
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
   "hint_hi": "मुझे दो किलो प्याज दो.",
   "hint_tag": "v_w"
  },
  "two": {
   "syllables": [
    "two"
   ],
   "hint_hi": "दो",
   "hint_tag": "s_cluster"
  },
  "kilos": {
   "syllables": [
    "kilos"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "of": {
   "syllables": [
    "of"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "two": {
   "syllables": [
    "two"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "of": {
   "syllables": [
    "of"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "kilos": {
   "syllables": [
    "प्याज"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "of": {
   "syllables": [
    "of"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "two": {
   "syllables": [
    "two"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "of": {
   "syllables": [
    "of"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "kilos": {
   "syllables": [
    "प्याज"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "two": {
   "syllables": [
    "two"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "of": {
   "syllables": [
    "of"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
  },
  "two": {
   "syllables": [
    "two"
   ],
   "hint_hi": "प्याज",
   "hint_tag": "s_cluster"
```

### bank_01: I want to open a bank account.

**gemma3:4b**

```
```json
{
  "text": "I want to open a bank account.",
  "hi": "मैं एक बैंक खाता खोलना चाहता हूँ।",
  "chunks": [
    "I want to",
    "open a bank",
    "account."
  ],
  "words": {
    "i": {
      "syllables": [
        "i"
      ],
      "hint_hi": "आई",
      "hint_tag": "generic"
    },
    "want": {
      "syllables": [
        "want"
      ],
      "hint_hi": "वंट",
      "hint_tag": "generic"
    },
    "to": {
      "syllables": [
        "to"
      ],
      "hint_hi": "टू",
      "hint_tag": "generic"
    },
    "open": {
      "syllables": [
        "o-pen"
      ],
      "hint_hi": "ओपन",
      "hint_tag": "v_w"
    },
    "a": {
      "syllables": [
        "a"
      ],
      "hint_hi": "ए",
      "hint_tag": "generic"
    },
    "bank": {
      "syllables": [
        "bank"
      ],
      "hint_hi": "बैंक",
      "hint_tag": "generic"
    },
    "account": {
      "syllables": [
        "ac-count"
      ],
      "hint_hi": "अकाउंट",
      "hint_tag": "s_cluster"
    }
  }
}
```
```

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "मैं एक बैंक खाता खोलना चाहता हूँ.",
  "hi": "मैं एक बैंक खाता खोलना चाहता हूँ.",
  "chunks": [
    "I want to open a bank account.",
    "I want to open a bank account."
  ],
  "words": {
    "want": {
      "syllables": ["want"],
      "hint_hi": "चाहता हूँ",
      "hint_tag": "generic"
    },
    "to": {
      "syllables": ["to"],
      "hint_hi": "के लिए",
      "hint_tag": "generic"
    },
    "open": {
      "syllables": ["open"],
      "hint_hi": "खोलना",
      "hint_tag": "generic"
    },
    "a": {
      "syllables": ["a"],
      "hint_hi": "एक",
      "hint_tag": "generic"
    },
    "bank": {
      "syllables": ["bank"],
      "hint_hi": "बैंक",
      "hint_tag": "generic"
    },
    "account": {
      "syllables": ["account"],
      "hint_hi": "खाता",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "I want to open a bank account.",
 "hi": "मैं एक बैंक खाता खोलना चाहता हूँ.",
 "chunks": [
  "I want to",
  "open a bank account."
 ],
 "words": {
  "want": {
   "syllables": [
    "want"
   ],
   "hint_hi": "चाहता हूँ",
   "hint_tag": "v_w"
  },
  "to": {
   "syllables": [
    "to"
   ],
   "hint_hi": "खोलना",
   "hint_tag": "th"
  },
  "bank": {
   "syllables": [
    "bank"
   ],
   "hint_hi": "खाता",
   "hint_tag": "s_cluster"
  },
  "account": {
   "syllables": [
    "खाता"
   ],
   "hint_hi": "खोलना",
   "hint_tag": "s_cluster"
  }
 }
}
```

### bank_02: Where do I sign?

**gemma3:4b**

```
```json
{
 "text": "Where do I sign?",
 "hi": "मैं कहाँ हस्ताक्षर करूँ?",
 "chunks": [
  "Where do",
  "I sign?"
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
   "hint_tag": "generic"
  },
  "i": {
   "syllables": [
    "i"
   ],
   "hint_hi": "आई",
   "hint_tag": "short_vowel"
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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "Where do I sign?",
  "hi": "मैं कहाँ हस्ताक्षर करूँ?",
  "chunks": [
    "Where do I",
    "sign"
  ],
  "words": {
    "where": {
      "syllables": ["where"],
      "hint_hi": "वेयर",
      "hint_tag": "v_w"
    },
    "do": {
      "syllables": ["do"],
      "hint_hi": "करो",
      "hint_tag": "generic"
    },
    "sign": {
      "syllables": ["sign"],
      "hint_hi": "हस्ताक्षर",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "Where do I sign?",
 "hi": "मैं साइन कहाँ करूं?",
 "chunks": [
  "Where do",
  "I sign"
 ],
 "words": {
  "where": {
   "syllables": [
    "Where do"
   ],
   "hint_hi": "मैं साइन कहाँ करूं?",
   "hint_tag": "v_w"
  },
  "do": {
   "syllables": [
    "do"
   ],
   "hint_hi": "मैं साइन करूं?",
   "hint_tag": "th"
  },
  "sign": {
   "syllables": [
    "साइन"
   ],
   "hint_hi": "मैं साइन करूं?",
   "hint_tag": "s_cluster"
  },
  "where": {
   "syllables": [
    "Where do"
   ],
   "hint_hi": "मैं साइन कहाँ करूं?",
   "hint_tag": "v_w"
  },
  "do": {
   "syllables": [
    "do"
   ],
   "hint_hi": "मैं साइन करूं?",
   "hint_tag": "th"
  },
  "sign": {
   "syllables": [
    "साइन"
   ],
   "hint_hi": "मैं साइन करूं?",
   "hint_tag": "s_cluster"
  },
  "where": {
   "syllables": [
    "Where do"
   ],
   "hint_hi": "मैं साइन कहाँ करूं?",
   "hint_tag": "v_w"
  },
  "do": {
   "syllables": [
    "do"
   ],
   "hint_hi": "मैं साइन करूं?",
   "hint_tag": "th"
  },
  "sign": {
   "syllables": [
    "साइन"
   ],
   "hint_hi": "मैं साइन करूं?",
   "hint_tag": "s_cluster"
  },
  "where": {
   "syllables": [
    "Where do"
   ],
   "hint_hi": "मैं साइन कहाँ करूं?",
   "hint_tag": "v_w"
  },
  "do": {
   "syllables": [
    "do"
   ],
   "hint_hi": "मैं साइन करूं?",
   "hint_tag": "th"
  },
  "sign": {
   "syllables": [
    "साइन"
   ],
   "hint_hi": "मैं साइन करूं?",
   "hint_tag": "s_cluster"
  },
  "where": {
   "syllables": [
    "Where do"
   ],
   "hint_hi": "मैं साइन कहाँ करूं?",
   "hint_tag": "v_w"
  },
  "do": {
   "syllables": [
    "do"
   ],
   "hint_hi": "मैं साइन करूं?",
   "hint_tag": "th"
  },
  "sign": {
   "syllables":
```

### hospital_01: My child has had a fever since yesterday.

**gemma3:4b**

```
```json
{
 "text": "My child has had a fever since yesterday.",
 "hi": "मेरा बच्चा कल से बुखार से पीड़ित है।",
 "chunks": [
  "My child",
  "has had",
  "a fever",
  "since yesterday"
 ],
 "words": {
  "my": {
   "syllables": [
    "my"
   ],
   "hint_hi": "माय",
   "hint_tag": "generic"
  },
  "child": {
   "syllables": [
    "child"
   ],
   "hint_hi": "चाइल्ड",
   "hint_tag": "generic"
  },
  "has": {
   "syllables": [
    "has"
   ],
   "hint_hi": "हैस",
   "hint_tag": "v_w"
  },
  "had": {
   "syllables": [
    "had"
   ],
   "hint_hi": "हड",
   "hint_tag": "v_w"
  },
  "a": {
   "syllables": [
    "a"
   ],
   "hint_hi": "ए",
   "hint_tag": "generic"
  },
  "fever": {
   "syllables": [
    "fe-ver"
   ],
   "hint_hi": "फीवर",
   "hint_tag": "generic"
  },
  "since": {
   "syllables": [
    "since"
   ],
   "hint_hi": "सांस",
   "hint_tag": "generic"
  },
  "yesterday": {
   "syllables": [
    "yes-ter-day"
   ],
   "hint_hi": "यस्टरडे",
   "hint_tag": "generic"
  }
 }
}
```
```

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "मेरे बच्चे को कल से बुखार है.",
  "hi": "मेरे बच्चे को कल से बुखार है.",
  "chunks": [
    "My child has had a fever since yesterday.",
    "My child has had a fever since yesterday."
  ],
  "words": {
    "has": {
      "syllables": ["has"],
      "hint_hi": "है",
      "hint_tag": "generic"
    },
    "had": {
      "syllables": ["had"],
      "hint_hi": "था",
      "hint_tag": "generic"
    },
    "a": {
      "syllables": ["a"],
      "hint_hi": "एक",
      "hint_tag": "generic"
    },
    "fever": {
      "syllables": ["fever"],
      "hint_hi": "बुखार",
      "hint_tag": "generic"
    },
    "since": {
      "syllables": ["since"],
      "hint_hi": "से",
      "hint_tag": "generic"
    },
    "yesterday": {
      "syllables": ["yesterday"],
      "hint_hi": "कल",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "My child has had a fever since yesterday.",
 "hi": "मेरा बच्चा हाँटरदीय से ही बुखार से पीड़ित है.",
 "chunks": [
  "has had",
  "a fever",
  "since",
  "yesterday"
 ],
 "words": {
  "has had": {
    "syllables": [
    "has had"
   ],
    "hint_hi": "है",
    "hint_tag": "v_w"
  },
  "a": {
    "syllables": [
    "a"
   ],
    "hint_hi": "ए",
    "hint_tag": "th"
  },
  "fever": {
    "syllables": [
    "fever"
   ],
    "hint_hi": "बुखार",
    "hint_tag": "s_cluster"
  },
  "since": {
    "syllables": [
    "since",
    "yesterday"
   ],
    "hint_hi": "हाँ",
    "hint_tag": "s_cluster"
  },
  "yesterday": {
    "yesterday"
   ],
  "since": {
    "syllables": [
    "since",
    "yesterday"
   ],
    "hint_hi": "हाँ",
    "hint_tag": "th"
  }
 }
}
```

### hospital_02: Which doctor should I see?

**gemma3:4b**

```
```json
{
 "text": "Which doctor should I see?",
 "hi": "मुझे कौन डॉक्टर दिखना चाहिए?",
 "chunks": [
  "Which doctor",
  "should I",
  "see?"
 ],
 "words": {
  "which": {
   "syllables": [
    "which"
   ],
   "hint_hi": "वही",
   "hint_tag": "generic"
  },
  "doctor": {
   "syllables": [
    "doc-tor"
   ],
   "hint_hi": "डॉक्टर",
   "hint_tag": "generic"
  },
  "should": {
   "syllables": [
    "should"
   ],
   "hint_hi": "शौद",
   "hint_tag": "short_vowel"
  },
  "i": {
   "syllables": [
    "i"
   ],
   "hint_hi": "मैं",
   "hint_tag": "generic"
  },
  "see": {
   "syllables": [
    "see"
   ],
   "hint_hi": "देखा",
   "hint_tag": "generic"
  }
 }
}
```
```

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**

```
{
  "text": "कौन सा डॉक्टर मुझे देखना चाहिए?",
  "hi": "कौन सा डॉक्टर मुझे देखना चाहिए?",
  "chunks": [
    "Which doctor",
    "should I see?"
  ],
  "words": {
    "which": {
      "syllables": ["which"],
      "hint_hi": "कौन सा",
      "hint_tag": "generic"
    },
    "doctor": {
      "syllables": ["doctor"],
      "hint_hi": "डॉक्टर",
      "hint_tag": "generic"
    },
    "should": {
      "syllables": ["should"],
      "hint_hi": "चाहिए",
      "hint_tag": "generic"
    },
    "I": {
      "syllables": ["I"],
      "hint_hi": "मैं",
      "hint_tag": "generic"
    },
    "see": {
      "syllables": ["see"],
      "hint_hi": "देखना चाहिए",
      "hint_tag": "generic"
    }
  }
}
```

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**

```
{
 "text": "Which doctor should I see?",
 "hi": "जिस डॉक्टर को मैं देखूं?",
 "chunks": [
  "Which",
  "doctor",
  "should",
  "I",
  "see",
  "whom",
  "I",
  "see",
  "whom",
  "should",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom",
  "I",
  "see",
  "whom",
  "whom
```

## Task B: explaining the first mistake

### bus_01-1

- Sentence: Where is the bus stop? (बस स्टॉप कहाँ है?)
- Heard: Where is bus stop?
- First mistake: {'index': 2, 'word': 'the', 'status': 'MISSING', 'heard_as': None}

**gemma3:4b**: आप ने "the" शब्द नहीं बोला। 
इसे सही तरीके से बोलिए: "the" (जैसे - "the bus stop")।

**qwen3:4b-instruct**: आपने "the" कहा नहीं, इसलिए गलत सुनाया। "the" कहिए, जैसे "the bus stop"।

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: बस स्टॉप कहाँ है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "Where is the bus stop?" - "बस स्टॉप कहाँ है?"

### bus_01-2

- Sentence: Where is the bus stop? (बस स्टॉप कहाँ है?)
- Heard: Where is the boss stop?
- First mistake: {'index': 3, 'word': 'bus', 'status': 'WRONG', 'heard_as': 'boss'}

**gemma3:4b**: बस का शब्द, "बस" (bas) बोलिए। यह "बॉस" (boss) नहीं है।

**qwen3:4b-instruct**: आपने "bus" कहा "boss" जैसा, इसलिए गलत हुआ। आप "bus" कहिए, जैसे "बस"।

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: बस स्टॉप कहाँ है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "Where is the boss stop?"

### bus_01-3

- Sentence: Where is the bus stop? (बस स्टॉप कहाँ है?)
- Heard: Where is the bus shop?
- First mistake: {'index': 4, 'word': 'stop', 'status': 'WRONG', 'heard_as': 'shop'}

**gemma3:4b**: आप "स्टॉप" शब्द को सही तरीके से बोल रहे हैं। 
"स्टॉप" का उच्चारण "स्टॉप" ही होता है, "शॉप" नहीं।

**qwen3:4b-instruct**: आपने "stop" कहा "shop" जैसा, लेकिन सही तरीके से कहना चाहिए "stop"।

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: बस स्टॉप कहाँ है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "Where is the bus stop?" - "बस स्टॉप कहाँ है?"

### bus_02-4

- Sentence: When will the bus come? (बस कब आएगी?)
- Heard: When the bus will come?
- First mistake: {'index': 1, 'word': 'will', 'status': 'MISSING', 'heard_as': None}

**gemma3:4b**: आप ने "will" शब्द नहीं बोला। 
"Will" का सही उच्चारण है: "विल"।

**qwen3:4b-instruct**: आपने "will" कहा नहीं। इसे कहें जैसे "will"।

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: बस कब आएगी?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "जब आएगी"

### bus_03-5

- Sentence: One ticket to Patna, please. (पटना का एक टिकट दीजिए।)
- Heard: Van ticket to Patna, please.
- First mistake: {'index': 0, 'word': 'one', 'status': 'WRONG', 'heard_as': 'van'}

**gemma3:4b**: एक "एक" शब्द सही बोलिए। “एक” का मतलब “one” होता है।

**qwen3:4b-instruct**: आपने "एक" कहा था जो "वैन" है। आप "एक" कहिए, जैसे "एक टिकट"।

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: पटना का एक टिकट दीजिए।

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "One" (word 1 of 5) should be said as "van" (word 1 of 5).

### bus_04-6

- Sentence: How much is the ticket? (टिकट कितने का है?)
- Heard: How much the ticket?
- First mistake: {'index': 2, 'word': 'is', 'status': 'MISSING', 'heard_as': None}

**gemma3:4b**: टिकट कितने का है। “is” शब्द यहाँ नहीं है। आप इसे बोल सकते हैं, “is” नहीं।

**qwen3:4b-instruct**: आपने "is" कहा नहीं, इसलिए गलत है। आप कहें चाहिए, "How much is the ticket?"

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: टिकट कितने का है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "How much the ticket?"

### bus_05-7

- Sentence: Does this bus go to Bettiah? (क्या यह बस बेतिया जाती है?)
- Heard: Does this bus goes to Bettiah?
- First mistake: {'index': 3, 'word': 'go', 'status': 'WRONG', 'heard_as': 'goes'}

**gemma3:4b**: बस में “जाती” शब्द सही है। “जाती” का मतलब है “goes” । आप इसे धीरे से बोलिए।

**qwen3:4b-instruct**: आपने "go" कहा "goes" के जैसा, लेकिन सही तरीके से कहें "go"।

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: क्या यह बस बेतिया जाती है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "Does this bus go to Bettiah?" - "क्या यह बस बेतिया जाती है?"

### bus_05-8

- Sentence: Does this bus go to Bettiah? (क्या यह बस बेतिया जाती है?)
- Heard: Do this bus go to Bettiah?
- First mistake: {'index': 0, 'word': 'does', 'status': 'WRONG', 'heard_as': 'do'}

**gemma3:4b**: आप "does" शब्द को "does" बोल रहे हैं। 
"Does" का सही उच्चारण "dēs" है।

**qwen3:4b-instruct**: आपने "does" कहा था जैसे "do"। इसके लिए ठीक तरीके से कहें "does"।

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: क्या यह बस बेतिया जाती है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "क्या यह बस बेतिया जाती है?"
