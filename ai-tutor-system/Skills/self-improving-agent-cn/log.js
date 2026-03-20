#!/usr/bin/env node
/**
 * Self-Improving Agent - Log utilities
 * Records errors, corrections, best practices, and knowledge gaps
 */

const fs = require('fs');
const path = require('path');

const MEMORY_DIR = path.join(process.env.USERPROFILE || process.env.HOME, '.openclaw', 'memory', 'self-improving');

function ensureDir() {
  if (!fs.existsSync(MEMORY_DIR)) {
    fs.mkdirSync(MEMORY_DIR, { recursive: true });
  }
}

function appendToFile(filename, data) {
  const filePath = path.join(MEMORY_DIR, filename);
  const line = JSON.stringify({ timestamp: new Date().toISOString(), ...data }) + '\n';
  fs.appendFileSync(filePath, line);
}

function logError({ command, error, fix, context = '' }) {
  ensureDir();
  appendToFile('errors.jsonl', { type: 'error', command, error, fix, context });
  console.log(`✅ Error logged: ${command}`);
}

function logCorrection({ topic, wrong, correct, context = '' }) {
  ensureDir();
  appendToFile('corrections.jsonl', { type: 'correction', topic, wrong, correct, context });
  console.log(`✅ Correction logged: ${topic}`);
}

function logBestPractice({ category, practice, reason, context = '' }) {
  ensureDir();
  appendToFile('best_practices.jsonl', { type: 'best_practice', category, practice, reason, context });
  console.log(`✅ Best practice logged: ${category}`);
}

function logKnowledgeGap({ topic, outdated, current, context = '' }) {
  ensureDir();
  appendToFile('knowledge_gaps.jsonl', { type: 'knowledge_gap', topic, outdated, current, context });
  console.log(`✅ Knowledge gap logged: ${topic}`);
}

function searchMemory(query) {
  const files = ['errors.jsonl', 'corrections.jsonl', 'best_practices.jsonl', 'knowledge_gaps.jsonl'];
  const results = [];
  
  for (const file of files) {
    const filePath = path.join(MEMORY_DIR, file);
    if (!fs.existsSync(filePath)) continue;
    
    const content = fs.readFileSync(filePath, 'utf-8');
    const lines = content.trim().split('\n').filter(l => l);
    
    for (const line of lines) {
      try {
        const entry = JSON.parse(line);
        const text = JSON.stringify(entry).toLowerCase();
        if (text.includes(query.toLowerCase())) {
          results.push(entry);
        }
      } catch (e) {
        // Skip invalid JSON lines
      }
    }
  }
  
  return results;
}

// CLI usage
if (require.main === module) {
  const args = process.argv.slice(2);
  const action = args[0];
  
  if (action === 'error') {
    logError({
      command: args[1] || '',
      error: args[2] || '',
      fix: args[3] || '',
      context: args[4] || ''
    });
  } else if (action === 'correction') {
    logCorrection({
      topic: args[1] || '',
      wrong: args[2] || '',
      correct: args[3] || '',
      context: args[4] || ''
    });
  } else if (action === 'practice') {
    logBestPractice({
      category: args[1] || '',
      practice: args[2] || '',
      reason: args[3] || '',
      context: args[4] || ''
    });
  } else if (action === 'gap') {
    logKnowledgeGap({
      topic: args[1] || '',
      outdated: args[2] || '',
      current: args[3] || '',
      context: args[4] || ''
    });
  } else if (action === 'search') {
    const results = searchMemory(args[1] || '');
    console.log(JSON.stringify(results, null, 2));
  } else {
    console.log('Usage: node log.js <error|correction|practice|gap|search> [args...]');
  }
}

module.exports = { logError, logCorrection, logBestPractice, logKnowledgeGap, searchMemory };
