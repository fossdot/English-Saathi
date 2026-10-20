## Overview (6 threads)

| Model | Files MB | Zipped MB | Words right | Sentences all right | Place names right | Other words right | Mistakes caught | False alarms | Silence/noise → nothing | Median ms | p90 ms | 3 s clips ms | RTF | Load s | Peak RAM +MB |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| whisper-base.en-fp32-arm64 | 292.5 | 170.1 | 80.7% | 43.3% | 14.8% | 86.2% | 87.5% | 13.0% | 6/6 | 170 | 216 | 160 | 0.051 | 0.34 | 810 |
| whisper-base.en-fp32-x64 | 292.5 | 170.1 | 80.7% | 43.3% | 14.8% | 86.2% | 87.5% | 13.0% | 6/6 | 402 | 492 | 372 | 0.119 | 0.67 | 693 |
| whisper-base.en-int8-arm64 | 160.6 | 95.1 | 78.8% | 42.2% | 13.9% | 84.3% | 87.5% | 14.4% | 6/6 | 168 | 227 | 158 | 0.053 | 0.39 | 707 |
| whisper-base.en-int8-x64 | 160.6 | 95.1 | 79.7% | 43.7% | 13.9% | 85.2% | 87.5% | 16.0% | 6/6 | 410 | 512 | 377 | 0.125 | 0.62 | 568 |
| whisper-fq-compute-arm64 | 44.3 | – | 75.6% | 36.3% | 8.3% | 81.2% | 90.6% | 15.2% | 6/6 | 63 | 141 | 59 | 0.023 | 0.18 | 366 |
| whisper-fq-compute-x64 | 44.3 | – | 75.4% | 37.0% | 9.3% | 80.9% | 90.6% | 14.7% | 6/6 | 187 | 230 | 172 | 0.055 | 0.36 | 254 |
| whisper-small.en-fp32-arm64 | 969.5 | 567.5 | 87.0% | 57.8% | 33.3% | 91.4% | 89.6% | 7.9% | 6/6 | 424 | 544 | 395 | 0.128 | 0.85 | 1776 |
| whisper-small.en-fp32-x64 | 969.5 | 567.5 | 87.0% | 57.8% | 33.3% | 91.4% | 89.6% | 7.9% | 6/6 | 1321 | 1505 | 1236 | 0.386 | 1.86 | 1664 |
| whisper-small.en-int8-arm64 | 375.5 | 228.4 | 86.8% | 58.5% | 33.3% | 91.3% | 88.5% | 8.7% | 6/6 | 394 | 489 | 367 | 0.118 | 0.70 | 1472 |
| whisper-small.en-int8-x64 | 375.5 | 228.4 | 86.5% | 57.8% | 32.4% | 91.0% | 89.6% | 8.7% | 6/6 | 1392 | 2148 | 1338 | 0.444 | 1.28 | 954 |
| whispercpp-base.en-ac650-x64 | 148.0 | 133.8 | 80.3% | 42.6% | 16.7% | 85.6% | 84.4% | 12.5% | 6/6 | 354 | 366 | 351 | 0.105 | 0.08 | 254 |
| whispercpp-base.en-x64 | 148.0 | 133.8 | 79.8% | 41.1% | 22.2% | 84.6% | 83.3% | 13.9% | 5/6 | 1147 | 1193 | 1144 | 0.341 | 0.14 | 279 |
| whispercpp-small.en-arm64 | 487.6 | 445.6 | 86.0% | 56.7% | 31.5% | 90.5% | 85.4% | 7.6% | 6/6 | 580 | 598 | 578 | 0.172 | 0.11 | 679 |
| whispercpp-small.en-x64 | 487.6 | 445.6 | 86.0% | 56.3% | 31.5% | 90.5% | 85.4% | 7.6% | 6/6 | 3744 | 3802 | 3737 | 1.107 | 0.39 | 701 |
| whispercpp-small.en-x64sse42 | 487.6 | 445.6 | 85.9% | 56.3% | 31.5% | 90.4% | 85.4% | 7.6% | 6/6 | 2617 | 2721 | 2605 | 0.778 | 0.20 | 704 |
| whispercpp-tiny.en-arm64 | 77.7 | 70.5 | 75.5% | 37.0% | 10.2% | 80.9% | 89.6% | 15.8% | 6/6 | 94 | 109 | 94 | 0.029 | 0.03 | 160 |
| whispercpp-tiny.en-x64 | 77.7 | 70.5 | 75.5% | 37.0% | 10.2% | 80.9% | 89.6% | 15.8% | 6/6 | 569 | 592 | 566 | 0.171 | 0.09 | 182 |
| whispercpp-tiny.en-x64sse42 | 77.7 | 70.5 | 75.5% | 37.0% | 10.2% | 80.9% | 89.6% | 15.8% | 6/6 | 378 | 414 | 376 | 0.117 | 0.05 | 183 |

## Words right by voice (correct readings)

| Model | aman | lekha-deva | lekha-latin | rishi | samantha | tara |
|---|---|---|---|---|---|---|
| whisper-base.en-fp32-arm64 | 85.0% (53.3%) | 62.4% (28.9%) | 76.5% (31.1%) | 85.0% (55.6%) | 89.3% (53.3%) | 85.9% (37.8%) |
| whisper-base.en-fp32-x64 | 85.0% (53.3%) | 62.4% (28.9%) | 76.5% (31.1%) | 85.0% (55.6%) | 89.3% (53.3%) | 85.9% (37.8%) |
| whisper-base.en-int8-arm64 | 81.6% (51.1%) | 60.3% (28.9%) | 75.6% (31.1%) | 85.5% (55.6%) | 85.9% (48.9%) | 84.2% (37.8%) |
| whisper-base.en-int8-x64 | 83.3% (53.3%) | 60.7% (26.7%) | 76.9% (35.6%) | 85.5% (55.6%) | 87.6% (51.1%) | 84.2% (40.0%) |
| whisper-fq-compute-arm64 | 82.5% (48.9%) | 56.0% (20.0%) | 64.5% (26.7%) | 85.9% (46.7%) | 87.2% (46.7%) | 77.4% (28.9%) |
| whisper-fq-compute-x64 | 82.5% (48.9%) | 54.7% (20.0%) | 65.4% (26.7%) | 88.0% (51.1%) | 85.5% (46.7%) | 76.5% (28.9%) |
| whisper-small.en-fp32-arm64 | 88.9% (62.2%) | 72.2% (51.1%) | 84.2% (42.2%) | 95.3% (75.6%) | 89.7% (55.6%) | 91.5% (60.0%) |
| whisper-small.en-fp32-x64 | 88.9% (62.2%) | 72.2% (51.1%) | 84.2% (42.2%) | 95.3% (75.6%) | 89.7% (55.6%) | 91.5% (60.0%) |
| whisper-small.en-int8-arm64 | 88.5% (62.2%) | 71.4% (53.3%) | 84.2% (42.2%) | 95.3% (75.6%) | 89.7% (55.6%) | 91.9% (62.2%) |
| whisper-small.en-int8-x64 | 88.5% (62.2%) | 71.4% (51.1%) | 82.5% (40.0%) | 95.3% (75.6%) | 89.7% (55.6%) | 91.9% (62.2%) |
| whispercpp-base.en-ac650-x64 | 84.2% (51.1%) | 65.0% (35.6%) | 76.1% (28.9%) | 84.6% (53.3%) | 88.5% (48.9%) | 83.3% (37.8%) |
| whispercpp-base.en-x64 | 82.9% (48.9%) | 62.8% (33.3%) | 78.6% (31.1%) | 83.3% (46.7%) | 88.5% (51.1%) | 82.5% (35.6%) |
| whispercpp-small.en-arm64 | 89.3% (66.7%) | 72.6% (55.6%) | 80.3% (35.6%) | 92.3% (64.4%) | 90.2% (57.8%) | 91.0% (60.0%) |
| whispercpp-small.en-x64 | 89.3% (66.7%) | 73.1% (55.6%) | 80.3% (35.6%) | 92.3% (64.4%) | 89.7% (55.6%) | 91.0% (60.0%) |
| whispercpp-small.en-x64sse42 | 89.3% (66.7%) | 72.6% (55.6%) | 80.3% (35.6%) | 92.3% (64.4%) | 89.7% (55.6%) | 91.0% (60.0%) |
| whispercpp-tiny.en-arm64 | 81.6% (46.7%) | 54.3% (28.9%) | 68.8% (26.7%) | 81.6% (44.4%) | 86.3% (44.4%) | 80.3% (31.1%) |
| whispercpp-tiny.en-x64 | 81.6% (46.7%) | 54.3% (28.9%) | 68.8% (26.7%) | 81.6% (44.4%) | 86.3% (44.4%) | 80.3% (31.1%) |
| whispercpp-tiny.en-x64sse42 | 81.6% (46.7%) | 54.3% (28.9%) | 68.8% (26.7%) | 81.6% (44.4%) | 86.3% (44.4%) | 80.3% (31.1%) |

Each cell: words right (sentences fully right).

## Words right by speaking style (correct readings)

| Model | halting | normal | slow |
|---|---|---|---|
| whisper-base.en-fp32-arm64 | 78.8% (40.0%) | 79.7% (42.2%) | 83.5% (47.8%) |
| whisper-base.en-fp32-x64 | 78.8% (40.0%) | 79.7% (42.2%) | 83.5% (47.8%) |
| whisper-base.en-int8-arm64 | 79.1% (42.2%) | 76.1% (38.9%) | 81.4% (45.6%) |
| whisper-base.en-int8-x64 | 79.7% (43.3%) | 77.6% (41.1%) | 81.8% (46.7%) |
| whisper-fq-compute-arm64 | 71.2% (31.1%) | 76.3% (37.8%) | 79.3% (40.0%) |
| whisper-fq-compute-x64 | 70.9% (33.3%) | 75.9% (37.8%) | 79.5% (40.0%) |
| whisper-small.en-fp32-arm64 | 82.5% (47.8%) | 87.0% (57.8%) | 91.5% (67.8%) |
| whisper-small.en-fp32-x64 | 82.5% (47.8%) | 87.0% (57.8%) | 91.5% (67.8%) |
| whisper-small.en-int8-arm64 | 82.3% (48.9%) | 86.8% (58.9%) | 91.5% (67.8%) |
| whisper-small.en-int8-x64 | 81.6% (48.9%) | 87.0% (57.8%) | 91.0% (66.7%) |
| whispercpp-base.en-ac650-x64 | 76.3% (37.8%) | 79.7% (41.1%) | 84.8% (48.9%) |
| whispercpp-base.en-x64 | 75.0% (34.4%) | 79.7% (41.1%) | 84.6% (47.8%) |
| whispercpp-small.en-arm64 | 79.3% (43.3%) | 87.4% (60.0%) | 91.2% (66.7%) |
| whispercpp-small.en-x64 | 79.3% (42.2%) | 87.4% (60.0%) | 91.2% (66.7%) |
| whispercpp-small.en-x64sse42 | 79.1% (42.2%) | 87.4% (60.0%) | 91.2% (66.7%) |
| whispercpp-tiny.en-arm64 | 70.1% (34.4%) | 76.3% (32.2%) | 80.1% (44.4%) |
| whispercpp-tiny.en-x64 | 70.1% (34.4%) | 76.3% (32.2%) | 80.1% (44.4%) |
| whispercpp-tiny.en-x64sse42 | 70.1% (34.4%) | 76.3% (32.2%) | 80.1% (44.4%) |

Each cell: words right (sentences fully right).

## Words right by recording condition (correct readings)

| Model | clean | noisy | phone |
|---|---|---|---|
| whisper-base.en-fp32-arm64 | 88.0% (56.7%) | 77.1% (35.6%) | 76.9% (37.8%) |
| whisper-base.en-fp32-x64 | 88.0% (56.7%) | 77.1% (35.6%) | 76.9% (37.8%) |
| whisper-base.en-int8-arm64 | 87.8% (57.8%) | 72.2% (30.0%) | 76.5% (38.9%) |
| whisper-base.en-int8-x64 | 87.6% (57.8%) | 74.8% (35.6%) | 76.7% (37.8%) |
| whisper-fq-compute-arm64 | 85.9% (53.3%) | 70.9% (27.8%) | 69.9% (27.8%) |
| whisper-fq-compute-x64 | 86.1% (53.3%) | 70.1% (28.9%) | 70.1% (28.9%) |
| whisper-small.en-fp32-arm64 | 91.9% (66.7%) | 84.6% (53.3%) | 84.4% (53.3%) |
| whisper-small.en-fp32-x64 | 91.9% (66.7%) | 84.6% (53.3%) | 84.4% (53.3%) |
| whisper-small.en-int8-arm64 | 91.7% (66.7%) | 84.8% (55.6%) | 84.0% (53.3%) |
| whisper-small.en-int8-x64 | 91.9% (66.7%) | 83.8% (54.4%) | 84.0% (52.2%) |
| whispercpp-base.en-ac650-x64 | 87.6% (55.6%) | 78.4% (38.9%) | 74.8% (33.3%) |
| whispercpp-base.en-x64 | 86.5% (52.2%) | 78.2% (38.9%) | 74.6% (32.2%) |
| whispercpp-small.en-arm64 | 91.5% (64.4%) | 83.8% (54.4%) | 82.7% (51.1%) |
| whispercpp-small.en-x64 | 91.5% (64.4%) | 83.8% (53.3%) | 82.7% (51.1%) |
| whispercpp-small.en-x64sse42 | 91.5% (64.4%) | 83.5% (53.3%) | 82.7% (51.1%) |
| whispercpp-tiny.en-arm64 | 85.5% (55.6%) | 71.8% (28.9%) | 69.2% (26.7%) |
| whispercpp-tiny.en-x64 | 85.5% (55.6%) | 71.8% (28.9%) | 69.2% (26.7%) |
| whispercpp-tiny.en-x64sse42 | 85.5% (55.6%) | 71.8% (28.9%) | 69.2% (26.7%) |

Each cell: words right (sentences fully right).

## What was heard instead of the place names (correct readings of sentences 3 and 5)

- **whisper-base.en-fp32-arm64:** two ×19, but ×13, partner ×11, padna ×10, now ×9, ticker ×9, bethea ×6, thus ×6, on ×5, december ×5, beethya ×4, bepia ×4
- **whisper-base.en-fp32-x64:** two ×19, but ×13, partner ×11, padna ×10, now ×9, ticker ×9, bethea ×6, thus ×6, on ×5, december ×5, beethya ×4, bepia ×4
- **whisper-base.en-int8-arm64:** two ×19, but ×12, padna ×10, ticker ×9, now ×8, partner ×8, bethea ×7, on ×6, thus ×6, december ×5, take ×4, bepia ×4
- **whisper-base.en-int8-x64:** two ×19, but ×13, now ×9, ticker ×9, partner ×9, padna ×9, on ×7, bethea ×7, thus ×6, take ×5, december ×5, beethya ×4
- **whisper-fq-compute-arm64:** but ×33, two ×31, now ×29, thus ×12, take ×8, betia ×8, it ×6, a ×5, better ×5, here ×5, december ×5, partner ×4
- **whisper-fq-compute-x64:** but ×32, two ×30, now ×27, thus ×11, betia ×9, take ×8, december ×7, it ×6, here ×6, was ×6, padna ×5, better ×5
- **whisper-small.en-fp32-arm64:** two ×13, december ×9, padna ×7, bethea ×7, bechia ×7, bethia ×6, on ×5, thus ×5, betya ×4, own ×3, bepia ×3, betiya ×3
- **whisper-small.en-fp32-x64:** two ×13, december ×9, padna ×7, bethea ×7, bechia ×7, bethia ×6, on ×5, thus ×5, betya ×4, own ×3, bepia ×3, betiya ×3
- **whisper-small.en-int8-arm64:** two ×13, december ×9, padna ×7, bethea ×7, on ×5, thus ×5, bechia ×5, betya ×4, own ×3, bethya ×3, that ×3, was ×3
- **whisper-small.en-int8-x64:** two ×11, december ×9, bethea ×7, thus ×7, bechia ×7, padna ×6, on ×5, betya ×4, bethia ×4, own ×3, betiya ×3, that ×3
- **whispercpp-base.en-ac650-x64:** two ×20, but ×11, ticker ×10, padna ×10, now ×8, partner ×8, bethea ×6, betia ×5, thus ×5, december ×5, on ×4, better ×4
- **whispercpp-base.en-x64:** two ×25, but ×14, now ×7, bethea ×7, ticker ×5, putnam ×5, betia ×5, december ×5, app ×5, own ×4, on ×4, the ×4
- **whispercpp-small.en-arm64:** two ×12, bethea ×9, december ×8, bechia ×7, betya ×6, padna ×5, own ×4, thus ×4, the ×4, just ×4, on ×3, batia ×3
- **whispercpp-small.en-x64:** two ×12, bethea ×9, december ×8, bechia ×7, betya ×6, padna ×5, own ×4, thus ×4, the ×4, just ×4, on ×3, now ×3
- **whispercpp-small.en-x64sse42:** two ×12, bethea ×9, december ×8, bechia ×7, betya ×6, padna ×5, own ×4, thus ×4, the ×4, just ×4, on ×3, batia ×3
- **whispercpp-tiny.en-arm64:** but ×29, two ×21, now ×18, betia ×13, do ×8, december ×7, it ×6, too ×6, thus ×6, padna ×5, partner ×4, not ×4
- **whispercpp-tiny.en-x64:** but ×29, two ×21, now ×18, betia ×13, december ×7, it ×6, too ×6, thus ×6, padna ×5, partner ×4, not ×4, here ×4
- **whispercpp-tiny.en-x64sse42:** but ×29, two ×21, now ×18, betia ×13, december ×7, it ×6, too ×6, thus ×6, padna ×5, partner ×4, not ×4, here ×4

## Mistakes caught, per planted mistake (8 clips each: 4 voices × clean/noisy)

| Said | whisper-base.en-fp32-arm64 | whisper-base.en-fp32-x64 | whisper-base.en-int8-arm64 | whisper-base.en-int8-x64 | whisper-fq-compute-arm64 | whisper-fq-compute-x64 | whisper-small.en-fp32-arm64 | whisper-small.en-fp32-x64 | whisper-small.en-int8-arm64 | whisper-small.en-int8-x64 | whispercpp-base.en-ac650-x64 | whispercpp-base.en-x64 | whispercpp-small.en-arm64 | whispercpp-small.en-x64 | whispercpp-small.en-x64sse42 | whispercpp-tiny.en-arm64 | whispercpp-tiny.en-x64 | whispercpp-tiny.en-x64sse42 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Do this bus go to Bettiah? | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 |
| Does this bus go Bettiah? | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 |
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
