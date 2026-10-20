# LLM benchmark summary

Speech model in the same process: loaded in 0.61 s, process 39 → 155 MB.

| Model | Params | Quant | Load s | A: first token s | A: tokens/s | A: time per sentence s | B: first token s | B: tokens/s | B: time per answer s | Prompt tokens/s | Peak RAM Ollama MB | Peak RAM speech process MB | A: valid JSON (repaired) | A: all checks | B: Devanagari | B: ≤2 sentences | Speech still works |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| gemma3:1b | 999.89M | Q4_K_M | 1.664 | 0.94 | 11.8 | 27.89 | 1.98 | 12.95 | 4.55 | 612.25 | 1084 | 130 | 10/10 (10) | 2/10 | 8/8 | 2/8 | yes |
| gemma3:4b | 3.9B | Q4_K_M | 4.088 | 18.16 | 3.58 | 110.5 | 15.92 | 2.67 | 25.83 | 31.25 | 2936 | 130 | 10/10 (10) | 10/10 | 8/8 | 6/8 | yes |
| hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M | 2.86B | Q4_K_M | 4.227 | 2.37 | 4.44 | 74.26 | 1.83 | 9.19 | 3.07 | 251.7 | 2333 | 129 | 4/10 (7) | 4/10 | 5/8 | 5/8 | yes |
| hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF | 4.19B | Q4_K_M | 8.619 | 2.85 | 3.23 | 94.37 | 3.58 | 4.01 | 6.49 | 203.55 | 2709 | 225 | 10/10 (10) | 6/10 | 8/8 | 8/8 | yes |
| llama3.2:1b-instruct-q4_K_M | 1.2B | Q4_K_M | 1.641 | 0.74 | 13.27 | 24.25 | 1.01 | 15.29 | 3.71 | 715.6 | 1169 | 207 | 4/10 (10) | 2/10 | 8/8 | 7/8 | yes |
| llama3.2:3b | 3.2B | Q4_K_M | 2.895 | 2.08 | 5.19 | 54.97 | 2.68 | 5.83 | 9.42 | 252.75 | 2640 | 149 | 0/10 (10) | 0/10 | 8/8 | 8/8 | yes |
| nemotron-mini | 4.2B | Q4_K_M | 3.852 | 1.72 | 5.18 | 52.95 | 2.52 | 6.7 | 4.04 | 338.35 | 2724 | 127 | 10/10 (10) | 4/10 | 8/8 | 8/8 | yes |
| phi4-mini | 3.8B | Q4_K_M | 3.824 | 1.72 | 4.56 | 50.74 | 3.51 | 4.89 | 7.55 | 296.25 | 3151 | 127 | 10/10 (10) | 4/10 | 8/8 | 7/8 | yes |
| qwen2.5:3b | 3.1B | Q4_K_M | 2.127 | 1.9 | 5.94 | 42.28 | 2.8 | 6.35 | 14.4 | 276.75 | 2300 | 127 | 10/10 (10) | 10/10 | 8/8 | 6/8 | yes |
| qwen3:1.7b | 2.0B | Q4_K_M | 2.417 | 1.77 | 9.39 | 34.21 | 1.53 | 10.64 | 10.02 | 301.35 | 1996 | 132 | 9/10 (9) | 4/10 | 8/8 | 6/8 | yes |
| qwen3:4b | 4.0B | Q4_K_M | 3.262 | 3.19 | 3.69 | 223.91 | 3.54 | 4.55 | 39.35 | 174.8 | 3246 | 130 | 0/10 (0) | 0/10 | 7/8 | 0/8 | yes |
| hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M | 1.66B | Q4_K_M | 1.873 | 1.45 | 7.62 | 42.92 | 1.76 | 8.75 | 9.07 | 417.45 | 1588 | 149 | 6/10 (10) | 2/10 | 8/8 | 0/8 | yes |
| hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M | 3.15B | Q4_K_M | 4.213 | 5.17 | 3.93 | 32.42 | 5.37 | 4.13 | 18.7 | 131.6 | 2980 | 150 | 2/10 (2) | 0/10 | 8/8 | 2/8 | yes |
| qwen3:4b-instruct | 4.0B | Q4_K_M | 6.07 | 4.15 | 2.68 | 96.4 | 5.21 | 3.06 | 22.79 | 126.95 | 3252 | 213 | 10/10 (10) | 8/10 | 8/8 | 7/8 | yes |

Medians over the cases of each task. "All checks" is mechanical only: valid JSON, the right keys, Devanagari present, chunks covering the sentence, word keys from the sentence, valid hint tags. It says nothing about whether the Hindi is good.

Answers cut off by the token limit (done_reason "length"): hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M: length, stop; llama3.2:1b-instruct-q4_K_M: length, stop; llama3.2:3b: length, stop; qwen3:1.7b: length, stop; qwen3:4b: length; hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M: length, stop
