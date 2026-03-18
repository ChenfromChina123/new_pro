import urllib.request
import csv
import io

url = "https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv"
print(f"Downloading {url}...")
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
response = urllib.request.urlopen(req)

print("Reading CSV...")
text = response.read().decode('utf-8')
csv_reader = csv.reader(io.StringIO(text))
header = next(csv_reader)

words = []
for row in csv_reader:
    if len(row) < 10:
        continue
    word = row[0]
    definition = row[3] # translation
    pos = row[4]
    tag = row[7]
    bnc = row[8]
    frq = row[9]

    if not word.isalpha():
        continue

    freq_val = 0
    if bnc and bnc.isdigit():
        freq_val = int(bnc)
    elif frq and frq.isdigit():
        freq_val = int(frq)

    if freq_val > 0:
        words.append({
            'word': word,
            'definition': definition.replace('\n', ' | ').replace('"', "'"),
            'pos': pos,
            'tag': tag,
            'freq': freq_val
        })

print(f"Total valid words: {len(words)}")
# Sort by frequency (assuming lower number means higher frequency in BNC/FRQ rank)
words.sort(key=lambda x: x['freq'])
top_words = words[:3000]

print("Generating SQL...")
with open('aispring/src/main/resources/db/migration/V3_9__import_ecdict_words.sql', 'w', encoding='utf-8') as f:
    f.write("INSERT IGNORE INTO public_vocabulary_words (word, language, definition, part_of_speech, tag, usage_count, created_at) VALUES\n")

    values = []
    for w in top_words:
        word = w['word'].replace("'", "''")
        definition = w['definition'].replace("'", "''")
        if not definition: definition = "No definition"
        pos = w['pos'].replace("'", "''")
        if not pos: pos = 'unknown'
        tag = w['tag'].replace("'", "''")

        values.append(f"('{word}', 'en', '{definition}', '{pos}', '{tag}', 0, NOW())")

    # Batch insert in chunks of 1000
    chunk_size = 1000
    for i in range(0, len(values), chunk_size):
        chunk = values[i:i+chunk_size]
        f.write(",\n".join(chunk) + ";\n")
        if i + chunk_size < len(values):
            f.write("INSERT IGNORE INTO public_vocabulary_words (word, language, definition, part_of_speech, tag, usage_count, created_at) VALUES\n")

print("Done generating V3_9__import_ecdict_words.sql")
