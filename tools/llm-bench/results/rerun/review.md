# LLM outputs for review

Please score each output from 1 (unusable) to 5 (ready to use) in scoring-sheet.csv. Task A: is the Hindi natural for a villager in Bihar, are the chunks sensible, are the sound hints right? Task B: would a slow Hindi reader understand what went wrong and how to fix it?

## Task A: lesson content

### bus_02: When will the bus come?

Reference from the pilot (human): hi = बस कब आएगी?; chunks = ['When will', 'the bus come']

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

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: बस स्टॉप कहाँ है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "Where is the bus stop?" - "बस स्टॉप कहाँ है?"

### bus_01-2

- Sentence: Where is the bus stop? (बस स्टॉप कहाँ है?)
- Heard: Where is the boss stop?
- First mistake: {'index': 3, 'word': 'bus', 'status': 'WRONG', 'heard_as': 'boss'}

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: बस स्टॉप कहाँ है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "Where is the boss stop?"

### bus_01-3

- Sentence: Where is the bus stop? (बस स्टॉप कहाँ है?)
- Heard: Where is the bus shop?
- First mistake: {'index': 4, 'word': 'stop', 'status': 'WRONG', 'heard_as': 'shop'}

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: बस स्टॉप कहाँ है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "Where is the bus stop?" - "बस स्टॉप कहाँ है?"

### bus_02-4

- Sentence: When will the bus come? (बस कब आएगी?)
- Heard: When the bus will come?
- First mistake: {'index': 1, 'word': 'will', 'status': 'MISSING', 'heard_as': None}

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: बस कब आएगी?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "जब आएगी"

### bus_03-5

- Sentence: One ticket to Patna, please. (पटना का एक टिकट दीजिए।)
- Heard: Van ticket to Patna, please.
- First mistake: {'index': 0, 'word': 'one', 'status': 'WRONG', 'heard_as': 'van'}

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: पटना का एक टिकट दीजिए।

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "One" (word 1 of 5) should be said as "van" (word 1 of 5).

### bus_04-6

- Sentence: How much is the ticket? (टिकट कितने का है?)
- Heard: How much the ticket?
- First mistake: {'index': 2, 'word': 'is', 'status': 'MISSING', 'heard_as': None}

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: टिकट कितने का है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "How much the ticket?"

### bus_05-7

- Sentence: Does this bus go to Bettiah? (क्या यह बस बेतिया जाती है?)
- Heard: Does this bus goes to Bettiah?
- First mistake: {'index': 3, 'word': 'go', 'status': 'WRONG', 'heard_as': 'goes'}

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: क्या यह बस बेतिया जाती है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "Does this bus go to Bettiah?" - "क्या यह बस बेतिया जाती है?"

### bus_05-8

- Sentence: Does this bus go to Bettiah? (क्या यह बस बेतिया जाती है?)
- Heard: Do this bus go to Bettiah?
- First mistake: {'index': 0, 'word': 'does', 'status': 'WRONG', 'heard_as': 'do'}

**hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF**: क्या यह बस बेतिया जाती है?

**hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M**: "क्या यह बस बेतिया जाती है?"
