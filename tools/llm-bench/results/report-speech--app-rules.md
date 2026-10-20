## Overview (6 threads) (app rules: place names unscored, homophones equal)

| Model | Files MB | Zipped MB | Words right | Sentences all right | Place names right | Other words right | Mistakes caught | False alarms | Silence/noise → nothing | Median ms | p90 ms | 3 s clips ms | RTF | Load s | Peak RAM +MB |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| whisper-base.en-fp32-arm64 | 292.5 | 170.1 | 87.7% | 65.2% | – | 87.7% | 86.5% | 11.7% | 6/6 | 170 | 216 | 160 | 0.051 | 0.34 | 810 |
| whisper-base.en-fp32-x64 | 292.5 | 170.1 | 87.7% | 65.2% | – | 87.7% | 86.5% | 11.7% | 6/6 | 402 | 492 | 372 | 0.119 | 0.67 | 693 |
| whisper-base.en-int8-arm64 | 160.6 | 95.1 | 85.9% | 63.3% | – | 85.9% | 86.5% | 13.3% | 6/6 | 168 | 227 | 158 | 0.053 | 0.39 | 707 |
| whisper-base.en-int8-x64 | 160.6 | 95.1 | 86.7% | 65.2% | – | 86.7% | 86.5% | 14.9% | 6/6 | 410 | 512 | 377 | 0.125 | 0.62 | 568 |
| whisper-fq-compute-arm64 | 44.3 | – | 83.6% | 56.7% | – | 83.6% | 90.6% | 13.9% | 6/6 | 63 | 141 | 59 | 0.023 | 0.18 | 366 |
| whisper-fq-compute-x64 | 44.3 | – | 83.4% | 57.4% | – | 83.4% | 90.6% | 13.3% | 6/6 | 187 | 230 | 172 | 0.055 | 0.36 | 254 |
| whisper-small.en-fp32-arm64 | 969.5 | 567.5 | 92.4% | 78.5% | – | 92.4% | 89.6% | 7.6% | 6/6 | 424 | 544 | 395 | 0.128 | 0.85 | 1776 |
| whisper-small.en-fp32-x64 | 969.5 | 567.5 | 92.4% | 78.5% | – | 92.4% | 89.6% | 7.6% | 6/6 | 1321 | 1505 | 1236 | 0.386 | 1.86 | 1664 |
| whisper-small.en-int8-arm64 | 375.5 | 228.4 | 92.3% | 79.3% | – | 92.3% | 88.5% | 8.2% | 6/6 | 394 | 489 | 367 | 0.118 | 0.70 | 1472 |
| whisper-small.en-int8-x64 | 375.5 | 228.4 | 91.9% | 78.1% | – | 91.9% | 89.6% | 8.4% | 6/6 | 1392 | 2148 | 1338 | 0.444 | 1.28 | 954 |
| whispercpp-base.en-ac650-x64 | 148.0 | 133.8 | 87.3% | 64.8% | – | 87.3% | 83.3% | 12.2% | 6/6 | 354 | 366 | 351 | 0.105 | 0.08 | 254 |
| whispercpp-base.en-x64 | 148.0 | 133.8 | 86.5% | 62.2% | – | 86.5% | 82.3% | 13.6% | 5/6 | 1147 | 1193 | 1144 | 0.341 | 0.14 | 279 |
| whispercpp-small.en-arm64 | 487.6 | 445.6 | 91.4% | 77.0% | – | 91.4% | 85.4% | 7.3% | 6/6 | 580 | 598 | 578 | 0.172 | 0.11 | 679 |
| whispercpp-small.en-x64 | 487.6 | 445.6 | 91.4% | 76.7% | – | 91.4% | 85.4% | 7.3% | 6/6 | 3744 | 3802 | 3737 | 1.107 | 0.39 | 701 |
| whispercpp-small.en-x64sse42 | 487.6 | 445.6 | 91.4% | 76.7% | – | 91.4% | 85.4% | 7.3% | 6/6 | 2617 | 2721 | 2605 | 0.778 | 0.20 | 704 |
| whispercpp-tiny.en-arm64 | 77.7 | 70.5 | 82.9% | 56.3% | – | 82.9% | 89.6% | 14.7% | 6/6 | 94 | 109 | 94 | 0.029 | 0.03 | 160 |
| whispercpp-tiny.en-x64 | 77.7 | 70.5 | 82.9% | 56.3% | – | 82.9% | 89.6% | 14.7% | 6/6 | 569 | 592 | 566 | 0.171 | 0.09 | 182 |
| whispercpp-tiny.en-x64sse42 | 77.7 | 70.5 | 82.9% | 56.3% | – | 82.9% | 89.6% | 14.7% | 6/6 | 378 | 414 | 376 | 0.117 | 0.05 | 183 |

## Words right by voice (correct readings)

| Model | aman | lekha-deva | lekha-latin | rishi | samantha | tara |
|---|---|---|---|---|---|---|
| whisper-base.en-fp32-arm64 | 93.1% (80.0%) | 67.1% (31.1%) | 81.0% (42.2%) | 93.5% (75.6%) | 98.1% (91.1%) | 93.5% (71.1%) |
| whisper-base.en-fp32-x64 | 93.1% (80.0%) | 67.1% (31.1%) | 81.0% (42.2%) | 93.5% (75.6%) | 98.1% (91.1%) | 93.5% (71.1%) |
| whisper-base.en-int8-arm64 | 88.9% (73.3%) | 65.7% (31.1%) | 81.0% (42.2%) | 93.5% (75.6%) | 94.4% (86.7%) | 91.7% (71.1%) |
| whisper-base.en-int8-x64 | 91.2% (77.8%) | 65.3% (28.9%) | 82.4% (46.7%) | 93.5% (75.6%) | 96.3% (88.9%) | 91.7% (73.3%) |
| whisper-fq-compute-arm64 | 92.6% (80.0%) | 62.0% (22.2%) | 71.3% (31.1%) | 93.1% (68.9%) | 95.4% (80.0%) | 87.5% (57.8%) |
| whisper-fq-compute-x64 | 92.6% (80.0%) | 60.2% (22.2%) | 72.2% (31.1%) | 94.9% (75.6%) | 94.0% (77.8%) | 86.6% (57.8%) |
| whisper-small.en-fp32-arm64 | 94.0% (86.7%) | 75.9% (51.1%) | 89.4% (55.6%) | 99.1% (95.6%) | 98.6% (93.3%) | 97.7% (88.9%) |
| whisper-small.en-fp32-x64 | 94.0% (86.7%) | 75.9% (51.1%) | 89.4% (55.6%) | 99.1% (95.6%) | 98.6% (93.3%) | 97.7% (88.9%) |
| whisper-small.en-int8-arm64 | 94.4% (88.9%) | 75.0% (53.3%) | 88.9% (55.6%) | 99.1% (95.6%) | 98.6% (93.3%) | 97.7% (88.9%) |
| whisper-small.en-int8-x64 | 94.0% (86.7%) | 74.5% (51.1%) | 87.5% (53.3%) | 99.1% (95.6%) | 98.6% (93.3%) | 97.7% (88.9%) |
| whispercpp-base.en-ac650-x64 | 92.1% (77.8%) | 69.0% (37.8%) | 80.6% (42.2%) | 93.1% (73.3%) | 97.2% (86.7%) | 91.7% (71.1%) |
| whispercpp-base.en-x64 | 90.7% (75.6%) | 66.7% (33.3%) | 82.4% (44.4%) | 91.2% (64.4%) | 97.2% (86.7%) | 90.7% (68.9%) |
| whispercpp-small.en-arm64 | 94.0% (88.9%) | 76.4% (55.6%) | 84.7% (48.9%) | 97.2% (86.7%) | 99.1% (95.6%) | 97.2% (86.7%) |
| whispercpp-small.en-x64 | 94.0% (88.9%) | 76.9% (55.6%) | 84.7% (48.9%) | 97.2% (86.7%) | 98.6% (93.3%) | 97.2% (86.7%) |
| whispercpp-small.en-x64sse42 | 94.0% (88.9%) | 76.4% (55.6%) | 84.7% (48.9%) | 97.2% (86.7%) | 98.6% (93.3%) | 97.2% (86.7%) |
| whispercpp-tiny.en-arm64 | 91.7% (75.6%) | 58.8% (28.9%) | 75.9% (33.3%) | 87.5% (57.8%) | 94.9% (77.8%) | 88.9% (64.4%) |
| whispercpp-tiny.en-x64 | 91.7% (75.6%) | 58.8% (28.9%) | 75.9% (33.3%) | 87.5% (57.8%) | 94.9% (77.8%) | 88.9% (64.4%) |
| whispercpp-tiny.en-x64sse42 | 91.7% (75.6%) | 58.8% (28.9%) | 75.9% (33.3%) | 87.5% (57.8%) | 94.9% (77.8%) | 88.9% (64.4%) |

Each cell: words right (sentences fully right).

## Words right by speaking style (correct readings)

| Model | halting | normal | slow |
|---|---|---|---|
| whisper-base.en-fp32-arm64 | 85.4% (58.9%) | 87.3% (65.6%) | 90.5% (71.1%) |
| whisper-base.en-fp32-x64 | 85.4% (58.9%) | 87.3% (65.6%) | 90.5% (71.1%) |
| whisper-base.en-int8-arm64 | 85.9% (60.0%) | 83.1% (61.1%) | 88.7% (68.9%) |
| whisper-base.en-int8-x64 | 86.3% (61.1%) | 84.7% (63.3%) | 89.1% (71.1%) |
| whisper-fq-compute-arm64 | 79.4% (46.7%) | 84.3% (60.0%) | 87.3% (63.3%) |
| whisper-fq-compute-x64 | 79.4% (47.8%) | 83.8% (61.1%) | 87.0% (63.3%) |
| whisper-small.en-fp32-arm64 | 89.1% (66.7%) | 91.9% (80.0%) | 96.3% (88.9%) |
| whisper-small.en-fp32-x64 | 89.1% (66.7%) | 91.9% (80.0%) | 96.3% (88.9%) |
| whisper-small.en-int8-arm64 | 89.1% (67.8%) | 91.4% (81.1%) | 96.3% (88.9%) |
| whisper-small.en-int8-x64 | 88.2% (66.7%) | 91.7% (80.0%) | 95.8% (87.8%) |
| whispercpp-base.en-ac650-x64 | 83.3% (55.6%) | 87.0% (64.4%) | 91.4% (74.4%) |
| whispercpp-base.en-x64 | 82.4% (48.9%) | 86.3% (64.4%) | 90.7% (73.3%) |
| whispercpp-small.en-arm64 | 86.8% (64.4%) | 91.7% (78.9%) | 95.8% (87.8%) |
| whispercpp-small.en-x64 | 86.8% (63.3%) | 91.7% (78.9%) | 95.8% (87.8%) |
| whispercpp-small.en-x64sse42 | 86.6% (63.3%) | 91.7% (78.9%) | 95.8% (87.8%) |
| whispercpp-tiny.en-arm64 | 78.2% (47.8%) | 83.1% (54.4%) | 87.5% (66.7%) |
| whispercpp-tiny.en-x64 | 78.2% (47.8%) | 83.1% (54.4%) | 87.5% (66.7%) |
| whispercpp-tiny.en-x64sse42 | 78.2% (47.8%) | 83.1% (54.4%) | 87.5% (66.7%) |

Each cell: words right (sentences fully right).

## Words right by recording condition (correct readings)

| Model | clean | noisy | phone |
|---|---|---|---|
| whisper-base.en-fp32-arm64 | 94.9% (81.1%) | 84.3% (58.9%) | 84.0% (55.6%) |
| whisper-base.en-fp32-x64 | 94.9% (81.1%) | 84.3% (58.9%) | 84.0% (55.6%) |
| whisper-base.en-int8-arm64 | 95.4% (82.2%) | 79.2% (53.3%) | 83.1% (54.4%) |
| whisper-base.en-int8-x64 | 95.4% (82.2%) | 81.7% (60.0%) | 83.1% (53.3%) |
| whisper-fq-compute-arm64 | 94.4% (78.9%) | 79.2% (50.0%) | 77.3% (41.1%) |
| whisper-fq-compute-x64 | 94.7% (80.0%) | 78.2% (50.0%) | 77.3% (42.2%) |
| whisper-small.en-fp32-arm64 | 96.5% (86.7%) | 90.0% (73.3%) | 90.7% (75.6%) |
| whisper-small.en-fp32-x64 | 96.5% (86.7%) | 90.0% (73.3%) | 90.7% (75.6%) |
| whisper-small.en-int8-arm64 | 96.8% (87.8%) | 90.0% (74.4%) | 90.0% (75.6%) |
| whisper-small.en-int8-x64 | 96.5% (86.7%) | 88.9% (73.3%) | 90.3% (74.4%) |
| whispercpp-base.en-ac650-x64 | 94.7% (81.1%) | 85.0% (62.2%) | 82.2% (51.1%) |
| whispercpp-base.en-x64 | 93.3% (75.6%) | 85.0% (61.1%) | 81.2% (50.0%) |
| whispercpp-small.en-arm64 | 96.5% (86.7%) | 89.4% (73.3%) | 88.4% (71.1%) |
| whispercpp-small.en-x64 | 96.5% (86.7%) | 89.4% (72.2%) | 88.4% (71.1%) |
| whispercpp-small.en-x64sse42 | 96.5% (86.7%) | 89.1% (72.2%) | 88.4% (71.1%) |
| whispercpp-tiny.en-arm64 | 93.1% (80.0%) | 79.9% (47.8%) | 75.9% (41.1%) |
| whispercpp-tiny.en-x64 | 93.1% (80.0%) | 79.9% (47.8%) | 75.9% (41.1%) |
| whispercpp-tiny.en-x64sse42 | 93.1% (80.0%) | 79.9% (47.8%) | 75.9% (41.1%) |

Each cell: words right (sentences fully right).

## What was heard instead of the place names (correct readings of sentences 3 and 5)

- **whisper-base.en-fp32-arm64:** two ×19, patna ×16, but ×13, partner ×11, padna ×10, now ×9, ticker ×9, bethea ×6, thus ×6, on ×5, december ×5, beethya ×4
- **whisper-base.en-fp32-x64:** two ×19, patna ×16, but ×13, partner ×11, padna ×10, now ×9, ticker ×9, bethea ×6, thus ×6, on ×5, december ×5, beethya ×4
- **whisper-base.en-int8-arm64:** two ×19, patna ×15, but ×12, padna ×10, ticker ×9, now ×8, partner ×8, bethea ×7, on ×6, thus ×6, december ×5, take ×4
- **whisper-base.en-int8-x64:** two ×19, patna ×15, but ×13, now ×9, ticker ×9, partner ×9, padna ×9, on ×7, bethea ×7, thus ×6, take ×5, december ×5
- **whisper-fq-compute-arm64:** but ×33, two ×31, now ×29, thus ×12, patna ×9, take ×8, betia ×8, it ×6, a ×5, better ×5, here ×5, december ×5
- **whisper-fq-compute-x64:** but ×32, two ×30, now ×27, thus ×11, patna ×10, betia ×9, take ×8, december ×7, it ×6, here ×6, was ×6, padna ×5
- **whisper-small.en-fp32-arm64:** patna ×36, two ×13, december ×9, padna ×7, bethea ×7, bechia ×7, bethia ×6, on ×5, thus ×5, betya ×4, own ×3, bepia ×3
- **whisper-small.en-fp32-x64:** patna ×36, two ×13, december ×9, padna ×7, bethea ×7, bechia ×7, bethia ×6, on ×5, thus ×5, betya ×4, own ×3, bepia ×3
- **whisper-small.en-int8-arm64:** patna ×36, two ×13, december ×9, padna ×7, bethea ×7, on ×5, thus ×5, bechia ×5, betya ×4, own ×3, bethya ×3, that ×3
- **whisper-small.en-int8-x64:** patna ×35, two ×11, december ×9, bethea ×7, thus ×7, bechia ×7, padna ×6, on ×5, betya ×4, bethia ×4, own ×3, betiya ×3
- **whispercpp-base.en-ac650-x64:** two ×20, patna ×18, but ×11, ticker ×10, padna ×10, now ×8, partner ×8, bethea ×6, betia ×5, thus ×5, december ×5, on ×4
- **whispercpp-base.en-x64:** two ×25, patna ×24, but ×14, now ×7, bethea ×7, ticker ×5, putnam ×5, betia ×5, december ×5, app ×5, own ×4, on ×4
- **whispercpp-small.en-arm64:** patna ×34, two ×12, bethea ×9, december ×8, bechia ×7, betya ×6, padna ×5, own ×4, thus ×4, the ×4, just ×4, on ×3
- **whispercpp-small.en-x64:** patna ×34, two ×12, bethea ×9, december ×8, bechia ×7, betya ×6, padna ×5, own ×4, thus ×4, the ×4, just ×4, on ×3
- **whispercpp-small.en-x64sse42:** patna ×34, two ×12, bethea ×9, december ×8, bechia ×7, betya ×6, padna ×5, own ×4, thus ×4, the ×4, just ×4, on ×3
- **whispercpp-tiny.en-arm64:** but ×29, two ×21, now ×18, betia ×13, patna ×11, do ×8, december ×7, it ×6, too ×6, thus ×6, padna ×5, partner ×4
- **whispercpp-tiny.en-x64:** but ×29, two ×21, now ×18, betia ×13, patna ×11, december ×7, it ×6, too ×6, thus ×6, padna ×5, partner ×4, not ×4
- **whispercpp-tiny.en-x64sse42:** but ×29, two ×21, now ×18, betia ×13, patna ×11, december ×7, it ×6, too ×6, thus ×6, padna ×5, partner ×4, not ×4

## Mistakes caught, per planted mistake (8 clips each: 4 voices × clean/noisy)

| Said | whisper-base.en-fp32-arm64 | whisper-base.en-fp32-x64 | whisper-base.en-int8-arm64 | whisper-base.en-int8-x64 | whisper-fq-compute-arm64 | whisper-fq-compute-x64 | whisper-small.en-fp32-arm64 | whisper-small.en-fp32-x64 | whisper-small.en-int8-arm64 | whisper-small.en-int8-x64 | whispercpp-base.en-ac650-x64 | whispercpp-base.en-x64 | whispercpp-small.en-arm64 | whispercpp-small.en-x64 | whispercpp-small.en-x64sse42 | whispercpp-tiny.en-arm64 | whispercpp-tiny.en-x64 | whispercpp-tiny.en-x64sse42 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Do this bus go to Bettiah? | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 |
| Does this bus go Bettiah? | 7/8 | 7/8 | 7/8 | 7/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 7/8 | 7/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 |
| Does this bus goes to Bettiah? | 6/8 | 6/8 | 6/8 | 6/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 3/8 | 3/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 |
| How match is the ticket? | 0/8 | 0/8 | 0/8 | 0/8 | 0/8 | 0/8 | 4/8 | 4/8 | 4/8 | 4/8 | 0/8 | 0/8 | 0/8 | 0/8 | 0/8 | 0/8 | 0/8 | 0/8 |
| How much the ticket? | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 7/8 | 7/8 | 7/8 | 7/8 | 8/8 | 8/8 | 7/8 | 7/8 | 7/8 | 7/8 | 7/8 | 7/8 |
| One ticket Patna, please. | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 7/8 | 8/8 | 8/8 | 8/8 | 7/8 | 7/8 | 7/8 |
| Van ticket to Patna, please. | 7/8 | 7/8 | 7/8 | 7/8 | 8/8 | 8/8 | 6/8 | 6/8 | 6/8 | 6/8 | 8/8 | 8/8 | 6/8 | 6/8 | 6/8 | 8/8 | 8/8 | 8/8 |
| When the bus will come? | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 |
| When will the bus came? | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 |
| Where is bus stop? | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 7/8 | 8/8 | 7/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 |
| Where is the boss stop? | 7/8 | 7/8 | 7/8 | 7/8 | 7/8 | 7/8 | 5/8 | 5/8 | 5/8 | 5/8 | 7/8 | 6/8 | 5/8 | 5/8 | 5/8 | 8/8 | 8/8 | 8/8 |
| Where is the bus shop? | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 |

## Output on clips without speech

- **whisper-base.en-fp32-arm64:** "(buzzing)" ×3; "(wind blowing)" ×3; "so hard to" ×1; "like that" ×1; "(static)" ×1
- **whisper-base.en-fp32-x64:** "(buzzing)" ×3; "(wind blowing)" ×3; "so hard to" ×1; "like that" ×1; "(static)" ×1
- **whisper-base.en-int8-arm64:** "(buzzer)" ×3; "(wind blowing)" ×3; "(static)" ×2; "so hard to love them but I just" ×1
- **whisper-base.en-int8-x64:** "(wind blowing)" ×3; "(buzzer)" ×2; "(static)" ×2; "(buzzing)" ×1; "and" ×1
- **whisper-fq-compute-arm64:** "(wind blowing)" ×3; "(speaking in foreign language)" ×3; "[ Silence ]" ×2; "[BLANK_AUDIO]" ×1
- **whisper-fq-compute-x64:** "[ Sound Effects ]" ×3; "(speaking in foreign language)" ×3; "(wind blowing)" ×2; "[BLANK_AUDIO]" ×1
- **whisper-small.en-fp32-arm64:** "(static)" ×2; "(water rushing)" ×2; "[buzzing]" ×1; "(water flowing)" ×1; "[BLANK_AUDIO]" ×1; "(crowd chattering)" ×1; "(indistinct)" ×1
- **whisper-small.en-fp32-x64:** "(static)" ×2; "(water rushing)" ×2; "[buzzing]" ×1; "(water flowing)" ×1; "[BLANK_AUDIO]" ×1; "(crowd chattering)" ×1; "(indistinct)" ×1
- **whisper-small.en-int8-arm64:** "(static)" ×3; "(water rushing)" ×2; "[BLANK_AUDIO]" ×2; "(water running)" ×1; "(muffled speaking)" ×1
- **whisper-small.en-int8-x64:** "(water rushing)" ×3; "(static)" ×2; "[BLANK_AUDIO]" ×2; "[buzzing]" ×1; "(muffled speaking)" ×1
- **whispercpp-base.en-ac650-x64:** "[silence]" ×3; "[BLANK_AUDIO]" ×2; "[inaudible]" ×2; "[music]" ×1; "[Music]" ×1
- **whispercpp-base.en-x64:** "[BLANK_AUDIO]" ×5; "you" ×1; "(water rushing)" ×1; "(speaking in foreign language)" ×1; "[NON-ENGLISH SPEECH]" ×1
- **whispercpp-small.en-arm64:** "[BLANK_AUDIO]" ×6; "[INAUDIBLE]" ×2; "[INTERPOSING VOICES]" ×1
- **whispercpp-small.en-x64:** "[BLANK_AUDIO]" ×6; "[INAUDIBLE]" ×2; "[INTERPOSING VOICES]" ×1
- **whispercpp-small.en-x64sse42:** "[BLANK_AUDIO]" ×6; "[INAUDIBLE]" ×2; "[INTERPOSING VOICES]" ×1
- **whispercpp-tiny.en-arm64:** "[BLANK_AUDIO]" ×6; "(people talking)" ×2; "(speaking in foreign language)" ×1
- **whispercpp-tiny.en-x64:** "[BLANK_AUDIO]" ×6; "(people talking)" ×2; "(speaking in foreign language)" ×1
- **whispercpp-tiny.en-x64sse42:** "[BLANK_AUDIO]" ×6; "(people talking)" ×2; "(speaking in foreign language)" ×1
