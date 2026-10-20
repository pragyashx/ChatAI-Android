const pptxgen = require('pptxgenjs');

const pptx = new pptxgen();
pptx.layout = 'LAYOUT_WIDE';
pptx.author = 'Chiti Code';
pptx.subject = 'College demonstration of the Chiti Code Android app';
pptx.title = 'Chiti Code - AI Coding Assistant';
pptx.company = 'Chiti Code';
pptx.lang = 'en-US';
pptx.theme = {
  headFontFace: 'Cambria',
  bodyFontFace: 'Calibri',
  lang: 'en-US'
};
pptx.defineSlideMaster({
  title: 'MASTER',
  background: { color: '101522' },
  objects: []
});

const W = 13.333;
const H = 7.5;
const C = {
  bg: '101522',
  bg2: '151C2D',
  panel: '1B2437',
  panel2: '202B42',
  purple: '6D5DFB',
  purple2: '8B7EFF',
  teal: '0F766E',
  mint: '2DD4BF',
  white: 'F8FAFC',
  text: 'DCE5F2',
  muted: '8EA0B8',
  faint: '56657D',
  coral: 'FF8A7A',
  yellow: 'F7C873',
  green: '78D6A3',
  black: '0B0F17'
};
const FONT = 'Calibri';
const MONO = 'Courier New';

function tx(slide, text, x, y, w, h, opts = {}) {
  slide.addText(text, {
    x, y, w, h,
    margin: 0,
    fontFace: opts.fontFace || FONT,
    fontSize: opts.fontSize || 16,
    color: opts.color || C.text,
    bold: opts.bold || false,
    italic: opts.italic || false,
    align: opts.align || 'left',
    valign: opts.valign || 'mid',
    breakLine: false,
    fit: 'shrink',
    charSpacing: opts.charSpacing || 0,
    paraSpaceAfterPt: opts.paraSpaceAfterPt || 0,
    transparency: opts.transparency,
    bullet: opts.bullet,
    isTextBox: true
  });
}

function box(slide, x, y, w, h, fill, radius = 0.16, line = null, transparency = 0) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x, y, w, h,
    rectRadius: radius,
    fill: { color: fill, transparency },
    line: line ? { color: line, width: 1 } : { color: fill, transparency: 100 }
  });
}

function rect(slide, x, y, w, h, fill, line = null, transparency = 0) {
  slide.addShape(pptx.ShapeType.rect, {
    x, y, w, h,
    fill: { color: fill, transparency },
    line: line ? { color: line, width: 1 } : { color: fill, transparency: 100 }
  });
}

function circle(slide, x, y, d, fill, line = null, transparency = 0) {
  slide.addShape(pptx.ShapeType.ellipse, {
    x, y, w: d, h: d,
    fill: { color: fill, transparency },
    line: line ? { color: line, width: 1 } : { color: fill, transparency: 100 }
  });
}

function line(slide, x1, y1, x2, y2, color = C.faint, width = 1.2, arrow = false, dash = 'solid') {
  slide.addShape(pptx.ShapeType.line, {
    x: x1, y: y1, w: x2 - x1, h: y2 - y1,
    line: { color, width, dashType: dash, endArrowType: arrow ? 'triangle' : 'none' }
  });
}

function pill(slide, label, x, y, w, fill = C.panel2, color = C.mint) {
  box(slide, x, y, w, 0.28, fill, 0.12);
  tx(slide, label.toUpperCase(), x, y + 0.01, w, 0.25, {
    fontSize: 8.5, color, bold: true, align: 'center', charSpacing: 1.2
  });
}

function pageNum(slide, n, label) {
  tx(slide, `0${n}`, 12.28, 7.08, 0.4, 0.18, { fontSize: 9, color: C.faint, bold: true, align: 'right' });
  tx(slide, label.toUpperCase(), 0.62, 7.08, 2.2, 0.18, { fontSize: 8, color: C.faint, bold: true, charSpacing: 1.1 });
}

function title(slide, kicker, heading, subheading) {
  pill(slide, kicker, 0.62, 0.48, 1.45, C.panel2, C.mint);
  tx(slide, heading, 0.62, 0.92, 8.7, 0.56, { fontFace: 'Cambria', fontSize: 29, color: C.white, bold: true });
  if (subheading) tx(slide, subheading, 0.64, 1.56, 9.2, 0.32, { fontSize: 13.5, color: C.muted });
}

function iconBadge(slide, symbol, x, y, fill = C.purple, color = C.white, size = 0.46) {
  circle(slide, x, y, size, fill);
  tx(slide, symbol, x, y + 0.005, size, size - 0.01, { fontFace: MONO, fontSize: 14, color, bold: true, align: 'center' });
}

function bullet(slide, text, x, y, w, color = C.text, accent = C.mint, size = 14) {
  circle(slide, x, y + 0.08, 0.08, accent);
  tx(slide, text, x + 0.18, y, w - 0.18, 0.32, { fontSize: size, color });
}

function codeLine(slide, text, x, y, w, color = C.text, size = 10) {
  tx(slide, text, x, y, w, 0.19, { fontFace: MONO, fontSize: size, color });
}

function drawLogo(slide, x, y, scale = 1) {
  circle(slide, x, y, 0.66 * scale, C.purple);
  circle(slide, x + 0.12 * scale, y + 0.13 * scale, 0.42 * scale, C.bg);
  circle(slide, x + 0.22 * scale, y + 0.22 * scale, 0.08 * scale, C.mint);
  circle(slide, x + 0.36 * scale, y + 0.22 * scale, 0.08 * scale, C.mint);
  line(slide, x + 0.2 * scale, y + 0.38 * scale, x + 0.46 * scale, y + 0.38 * scale, C.mint, 1.4 * scale);
  line(slide, x + 0.33 * scale, y + 0.05 * scale, x + 0.33 * scale, y - 0.06 * scale, C.purple2, 1.2 * scale);
  circle(slide, x + 0.29 * scale, y - 0.1 * scale, 0.08 * scale, C.mint);
}

function drawPhone(slide, x, y, w, h, mode = 'welcome') {
  box(slide, x, y, w, h, C.black, 0.24, C.panel2);
  box(slide, x + 0.08, y + 0.12, w - 0.16, h - 0.24, C.bg2, 0.17);
  box(slide, x + w / 2 - 0.35, y + 0.045, 0.7, 0.07, C.black, 0.03);
  tx(slide, '9:41', x + 0.2, y + 0.17, 0.34, 0.14, { fontSize: 7, color: C.muted });
  tx(slide, 'Chiti Code', x + 0.27, y + 0.48, w - 0.54, 0.24, { fontSize: 11.5, color: C.white, bold: true });
  line(slide, x + 0.18, y + 0.78, x + w - 0.18, y + 0.78, C.panel2, 0.7);
  if (mode === 'welcome') {
    drawLogo(slide, x + w / 2 - 0.28, y + 1.02, 0.84);
    tx(slide, 'What can I help\nyou code?', x + 0.26, y + 1.82, w - 0.52, 0.55, { fontSize: 16, color: C.white, bold: true, align: 'center' });
    const prompts = ['Prime number in Python', 'Binary search in Java', 'Debug Kotlin error'];
    prompts.forEach((p, i) => {
      box(slide, x + 0.22, y + 2.65 + i * 0.48, w - 0.44, 0.35, C.panel, 0.08);
      tx(slide, p, x + 0.36, y + 2.71 + i * 0.48, w - 0.72, 0.18, { fontSize: 8.3, color: C.muted });
    });
  } else if (mode === 'chat') {
    box(slide, x + 0.26, y + 1.05, w - 0.52, 0.58, C.purple, 0.11);
    tx(slide, 'Write binary search in Java', x + 0.39, y + 1.18, w - 0.78, 0.23, { fontSize: 8.8, color: C.white });
    box(slide, x + 0.26, y + 1.82, w - 0.52, 2.35, C.panel, 0.11);
    tx(slide, 'Here is a Java implementation:', x + 0.39, y + 1.98, w - 0.78, 0.22, { fontSize: 8.5, color: C.text });
    box(slide, x + 0.39, y + 2.32, w - 0.78, 1.23, C.black, 0.06);
    codeLine(slide, 'int binarySearch(int[] a, int key) {', x + 0.5, y + 2.48, w - 1.0, C.mint, 6.5);
    codeLine(slide, '  int lo = 0, hi = a.length - 1;', x + 0.5, y + 2.7, w - 1.0, C.text, 6.5);
    codeLine(slide, '  while (lo <= hi) { ... }', x + 0.5, y + 2.92, w - 1.0, C.text, 6.5);
    codeLine(slide, '  return -1;', x + 0.5, y + 3.14, w - 1.0, C.text, 6.5);
    codeLine(slide, '}', x + 0.5, y + 3.36, w - 1.0, C.mint, 6.5);
    box(slide, x + 0.22, y + h - 0.67, w - 0.44, 0.38, C.panel, 0.1);
    tx(slide, 'Ask a follow-up...', x + 0.38, y + h - 0.58, w - 0.75, 0.18, { fontSize: 8, color: C.faint });
  } else {
    box(slide, x + 0.08, y + 0.42, 0.92, h - 0.66, C.panel, 0.13);
    tx(slide, '+  New chat', x + 0.2, y + 0.7, 0.68, 0.2, { fontSize: 8, color: C.mint, bold: true });
    tx(slide, 'TODAY', x + 0.2, y + 1.2, 0.7, 0.16, { fontSize: 6.5, color: C.faint, bold: true });
    tx(slide, 'Binary search in Java', x + 0.2, y + 1.46, 0.68, 0.28, { fontSize: 7.2, color: C.white });
    tx(slide, 'Prime number in Python', x + 0.2, y + 1.9, 0.68, 0.28, { fontSize: 7.2, color: C.muted });
    tx(slide, 'PREVIOUS 7 DAYS', x + 0.2, y + 2.42, 0.75, 0.16, { fontSize: 6.5, color: C.faint, bold: true });
    tx(slide, 'Kotlin null pointer', x + 0.2, y + 2.72, 0.68, 0.28, { fontSize: 7.2, color: C.muted });
    tx(slide, 'Conversation history', x + 1.25, y + 1.35, w - 1.55, 0.35, { fontSize: 12, color: C.white, bold: true });
    box(slide, x + 1.25, y + 2.02, w - 1.55, 1.35, C.panel, 0.11);
    tx(slide, 'Your saved chats stay\nready for the next question.', x + 1.46, y + 2.3, w - 1.95, 0.6, { fontSize: 9, color: C.text });
  }
}

function addCard(slide, x, y, w, h, badge, heading, body, accent = C.purple) {
  box(slide, x, y, w, h, C.panel, 0.16);
  iconBadge(slide, badge, x + 0.22, y + 0.22, accent, C.white, 0.38);
  tx(slide, heading, x + 0.74, y + 0.22, w - 0.94, 0.27, { fontSize: 14, color: C.white, bold: true });
  tx(slide, body, x + 0.22, y + 0.66, w - 0.44, h - 0.78, { fontSize: 10.7, color: C.muted, valign: 'top' });
}

// Slide 1: Introduction
{
  const slide = pptx.addSlide('MASTER');
  slide.background = { color: C.bg };
  circle(slide, 9.6, -1.0, 5.2, C.purple, null, 88);
  circle(slide, 10.55, 5.05, 3.9, C.teal, null, 91);
  pill(slide, 'ANDROID PROJECT', 0.68, 0.72, 1.72, C.panel2, C.mint);
  drawLogo(slide, 0.72, 1.62, 1.22);
  tx(slide, 'Chiti Code', 1.58, 1.63, 4.3, 0.55, { fontFace: 'Cambria', fontSize: 37, color: C.white, bold: true });
  tx(slide, 'AI coding assistance,\nmade mobile.', 0.72, 2.47, 5.6, 1.12, { fontFace: 'Cambria', fontSize: 31, color: C.white, bold: true });
  tx(slide, 'A conversational Android app that helps students and developers generate, understand, and debug code wherever they are.', 0.74, 3.85, 4.95, 0.76, { fontSize: 15, color: C.muted, valign: 'top' });
  pill(slide, 'KOTLIN', 0.74, 5.07, 0.82, C.panel, C.purple2);
  pill(slide, 'JETPACK COMPOSE', 1.66, 5.07, 1.55, C.panel, C.mint);
  pill(slide, 'ROOM', 3.35, 5.07, 0.72, C.panel, C.yellow);
  pill(slide, 'AI STREAMING', 4.17, 5.07, 1.28, C.panel, C.coral);
  tx(slide, 'COLLEGE DEMONSTRATION', 0.74, 6.33, 2.9, 0.22, { fontSize: 10, color: C.faint, bold: true, charSpacing: 1.2 });
  tx(slide, 'Version 1.4.0', 4.18, 6.33, 1.45, 0.22, { fontSize: 10, color: C.faint, align: 'right' });
  drawPhone(slide, 7.55, 0.66, 3.72, 5.96, 'welcome');
  box(slide, 11.62, 4.92, 1.05, 0.72, C.panel2, 0.12);
  tx(slide, '>_\ncode', 11.76, 5.04, 0.78, 0.44, { fontFace: MONO, fontSize: 11, color: C.mint, bold: true, align: 'center' });
  pageNum(slide, 1, 'Introduction');
  slide.addNotes('Introduce Chiti Code as a mobile-first coding assistant. Emphasize that the app is designed for quick programming help, not general-purpose chat.');
}

// Slide 2: Objectives
{
  const slide = pptx.addSlide('MASTER');
  slide.background = { color: C.bg };
  title(slide, '01 / WHY', 'From coding friction to a clear next step.', 'The project is built around one simple objective: reduce the time between a coding problem and a useful answer.');
  box(slide, 0.65, 2.2, 3.16, 3.9, C.panel, 0.18);
  pill(slide, 'THE PROBLEM', 0.93, 2.5, 1.33, C.bg2, C.coral);
  tx(slide, 'When learning or building on the go, users often get stuck on:', 0.93, 3.0, 2.48, 0.58, { fontSize: 15, color: C.white, bold: true, valign: 'top' });
  bullet(slide, 'Syntax and runtime errors', 0.95, 3.9, 2.52, C.text, C.coral, 12.5);
  bullet(slide, 'Unfamiliar algorithms', 0.95, 4.36, 2.52, C.text, C.coral, 12.5);
  bullet(slide, 'Long searches for examples', 0.95, 4.82, 2.52, C.text, C.coral, 12.5);
  tx(slide, 'A mobile workflow keeps help close to the moment of need.', 0.93, 5.48, 2.4, 0.4, { fontSize: 10.5, color: C.muted, italic: true, valign: 'top' });
  line(slide, 3.98, 4.04, 4.6, 4.04, C.mint, 2.3, true);
  tx(slide, 'TURN', 4.06, 3.65, 0.48, 0.2, { fontSize: 8, color: C.mint, bold: true, align: 'center' });
  box(slide, 4.8, 2.2, 7.86, 3.9, C.bg2, 0.18, C.panel2);
  pill(slide, 'PROJECT OBJECTIVES', 5.1, 2.5, 1.85, C.purple, C.white);
  tx(slide, 'Make coding support fast, focused, and reusable.', 5.1, 2.95, 6.7, 0.36, { fontSize: 20, color: C.white, bold: true });
  const objectives = [
    ['01', 'Accessible', 'Bring programming help to an Android phone.'],
    ['02', 'Contextual', 'Keep recent messages for meaningful follow-ups.'],
    ['03', 'Practical', 'Return usable code with explanations and formatting.'],
    ['04', 'Persistent', 'Save conversations so users can return to them later.']
  ];
  objectives.forEach((item, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 5.1 + col * 3.65;
    const y = 3.72 + row * 1.02;
    circle(slide, x, y + 0.03, 0.38, i === 1 ? C.teal : C.purple);
    tx(slide, item[0], x, y + 0.08, 0.38, 0.18, { fontFace: MONO, fontSize: 8, color: C.white, bold: true, align: 'center' });
    tx(slide, item[1], x + 0.54, y, 2.7, 0.22, { fontSize: 13, color: C.white, bold: true });
    tx(slide, item[2], x + 0.54, y + 0.3, 2.78, 0.38, { fontSize: 10.2, color: C.muted, valign: 'top' });
  });
  pageNum(slide, 2, 'Objectives');
  slide.addNotes('Explain the motivation before discussing implementation. The app addresses common student and developer pain points: debugging, algorithms, examples, and remembering prior work.');
}

// Slide 3: Functionality
{
  const slide = pptx.addSlide('MASTER');
  slide.background = { color: C.bg };
  title(slide, '02 / WHAT IT DOES', 'One chat, several coding workflows.', 'Users can move from a prompt to a formatted answer, then continue with a context-aware follow-up.');
  addCard(slide, 0.65, 2.2, 2.7, 1.52, '<>', 'Generate', 'Create programs, algorithms, and examples in the requested language.', C.purple);
  addCard(slide, 3.55, 2.2, 2.7, 1.52, '??', 'Explain', 'Break down concepts, code behavior, and complexity in simple terms.', C.teal);
  addCard(slide, 0.65, 3.94, 2.7, 1.52, '!!', 'Debug', 'Help identify errors and suggest practical fixes.', C.coral);
  addCard(slide, 3.55, 3.94, 2.7, 1.52, '[]', 'Remember', 'Keep local conversation history for later reference and follow-up questions.', C.yellow);
  box(slide, 6.72, 2.2, 5.93, 4.55, C.panel, 0.18);
  pill(slide, 'USER EXPERIENCE', 7.02, 2.5, 1.48, C.bg2, C.mint);
  tx(slide, 'Prompt in.\nUseful code out.', 7.02, 2.95, 2.35, 0.73, { fontFace: 'Cambria', fontSize: 22, color: C.white, bold: true, valign: 'top' });
  drawPhone(slide, 9.25, 2.48, 2.42, 3.87, 'chat');
  box(slide, 7.02, 4.02, 1.83, 1.74, C.bg2, 0.12);
  bullet(slide, 'Streaming answer', 7.22, 4.28, 1.45, C.text, C.mint, 10.5);
  bullet(slide, 'Markdown code', 7.22, 4.77, 1.45, C.text, C.mint, 10.5);
  bullet(slide, 'Follow-up context', 7.22, 5.26, 1.45, C.text, C.mint, 10.5);
  tx(slide, 'Scope: programming and software development', 7.02, 6.1, 4.1, 0.2, { fontSize: 9.5, color: C.faint, italic: true });
  pageNum(slide, 3, 'Functionality');
  slide.addNotes('Walk through the four core actions: generate, explain, debug, and remember. Then point to the phone mockup: the response streams in, renders as Markdown, and supports the next question.');
}

// Slide 4: Architecture and flow
{
  const slide = pptx.addSlide('MASTER');
  slide.background = { color: C.bg };
  title(slide, '03 / HOW IT WORKS', 'A prompt travels through a focused Android pipeline.', 'The architecture separates the interface, state, orchestration, remote AI response, and local persistence.');
  // Main flow boxes
  const nodes = [
    { x: 0.75, y: 3.0, w: 1.65, h: 1.1, top: 'USER', main: 'Coding prompt', sub: 'Question or example', color: C.coral },
    { x: 2.85, y: 3.0, w: 1.8, h: 1.1, top: 'UI', main: 'Compose screen', sub: 'Chat + input', color: C.purple },
    { x: 5.1, y: 3.0, w: 1.9, h: 1.1, top: 'STATE', main: 'ChatViewModel', sub: 'StateFlow', color: C.teal },
    { x: 7.45, y: 3.0, w: 2.05, h: 1.1, top: 'LOGIC', main: 'ChatRepository', sub: 'Context + stream', color: C.yellow }
  ];
  nodes.forEach((n, i) => {
    box(slide, n.x, n.y, n.w, n.h, C.panel, 0.16, n.color);
    pill(slide, n.top, n.x + 0.16, n.y + 0.16, 0.72, C.bg2, n.color);
    tx(slide, n.main, n.x + 0.16, n.y + 0.5, n.w - 0.32, 0.22, { fontSize: 13.2, color: C.white, bold: true });
    tx(slide, n.sub, n.x + 0.16, n.y + 0.79, n.w - 0.32, 0.18, { fontSize: 9.5, color: C.muted });
    if (i < nodes.length - 1) line(slide, n.x + n.w, 3.55, nodes[i + 1].x, 3.55, C.mint, 1.8, true);
  });
  // Branches
  box(slide, 10.05, 2.15, 2.58, 1.48, C.panel, 0.16, C.purple2);
  pill(slide, 'REMOTE', 10.28, 2.42, 0.75, C.bg2, C.purple2);
  tx(slide, 'OpenRouter API', 10.28, 2.83, 2.0, 0.23, { fontSize: 14, color: C.white, bold: true });
  tx(slide, 'Streaming model response', 10.28, 3.14, 1.95, 0.2, { fontSize: 10, color: C.muted });
  box(slide, 10.05, 4.12, 2.58, 1.48, C.panel, 0.16, C.teal);
  pill(slide, 'LOCAL', 10.28, 4.39, 0.68, C.bg2, C.teal);
  tx(slide, 'Room / SQLite', 10.28, 4.8, 2.0, 0.23, { fontSize: 14, color: C.white, bold: true });
  tx(slide, 'Conversations + messages', 10.28, 5.11, 1.98, 0.2, { fontSize: 10, color: C.muted });
  line(slide, 9.5, 3.28, 10.05, 2.88, C.purple2, 1.5, true);
  line(slide, 9.5, 3.84, 10.05, 4.75, C.teal, 1.5, true);
  // Response loop
  box(slide, 2.85, 5.65, 6.65, 0.58, C.bg2, 0.14);
  tx(slide, 'Streaming chunks  ->  StateFlow update  ->  Markdown response on screen', 3.08, 5.83, 6.18, 0.2, { fontFace: MONO, fontSize: 10.2, color: C.mint, align: 'center' });
  line(slide, 10.05, 5.8, 9.5, 5.98, C.mint, 1.4, true);
  tx(slide, 'Built with Kotlin  |  Jetpack Compose  |  Coroutines / Flow  |  OkHttp  |  Room', 0.78, 6.55, 10.8, 0.2, { fontSize: 10.5, color: C.faint, charSpacing: 0.15 });
  pageNum(slide, 4, 'Architecture');
  slide.addNotes('Use this diagram to explain the complete request lifecycle. The repository sends the prompt plus recent context to the remote model, while Room stores the local conversation. Streaming chunks update the Compose UI through StateFlow.');
}

// Slide 5: Demo and next steps
{
  const slide = pptx.addSlide('MASTER');
  slide.background = { color: C.bg };
  circle(slide, 11.2, -0.9, 3.8, C.teal, null, 90);
  title(slide, '04 / DEMO', 'Show the idea in under two minutes.', 'A short scenario demonstrates the complete value loop: ask, receive, continue, and return.');
  box(slide, 0.65, 2.28, 7.45, 3.95, C.panel, 0.18);
  pill(slide, 'LIVE SEQUENCE', 0.95, 2.58, 1.35, C.purple, C.white);
  const steps = [
    ['01', 'Ask', 'Select: "Write binary search in Java."'],
    ['02', 'Watch', 'Show the response streaming into a formatted code block.'],
    ['03', 'Continue', 'Ask: "Explain its time complexity."'],
    ['04', 'Return', 'Open history, switch chats, then create a new chat.']
  ];
  steps.forEach((s, i) => {
    const y = 3.12 + i * 0.68;
    circle(slide, 0.98, y, 0.34, i === 1 ? C.teal : C.purple);
    tx(slide, s[0], 0.98, y + 0.07, 0.34, 0.16, { fontFace: MONO, fontSize: 7.5, color: C.white, bold: true, align: 'center' });
    tx(slide, s[1], 1.52, y - 0.02, 1.05, 0.22, { fontSize: 13.5, color: C.white, bold: true });
    tx(slide, s[2], 2.62, y - 0.02, 4.95, 0.25, { fontSize: 11.5, color: C.muted });
    if (i < steps.length - 1) line(slide, 1.15, y + 0.36, 1.15, y + 0.64, C.faint, 1.2);
  });
  box(slide, 8.5, 2.28, 4.15, 3.95, C.bg2, 0.18, C.panel2);
  pill(slide, 'NEXT ITERATION', 8.8, 2.58, 1.42, C.teal, C.white);
  tx(slide, 'Make a good demo\neven stronger.', 8.8, 3.02, 3.1, 0.63, { fontFace: 'Cambria', fontSize: 21, color: C.white, bold: true, valign: 'top' });
  bullet(slide, 'In-app API settings', 8.82, 4.0, 3.12, C.text, C.mint, 11.5);
  bullet(slide, 'Search, stop, and regenerate', 8.82, 4.45, 3.12, C.text, C.mint, 11.5);
  bullet(slide, 'Cloud sync and accounts', 8.82, 4.9, 3.12, C.text, C.mint, 11.5);
  bullet(slide, 'Tests and offline support', 8.82, 5.35, 3.12, C.text, C.mint, 11.5);
  // tiny closing prompt card
  box(slide, 0.65, 6.48, 12.0, 0.42, C.purple, 0.12);
  tx(slide, 'The takeaway: Chiti Code turns a stuck moment into a guided coding conversation.', 0.88, 6.59, 11.55, 0.18, { fontSize: 11.5, color: C.white, bold: true, align: 'center' });
  pageNum(slide, 5, 'Demo');
  slide.addNotes('Run the live sequence in this order. Keep the explanation short and let the app demonstrate the value. End by mentioning future improvements, especially secure settings, search, response controls, cloud sync, and test coverage.');
}

pptx.writeFile({ fileName: 'Chiti_Code_Demo.pptx' });
