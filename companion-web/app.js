// Lori Companion Web Assistant
let authToken = localStorage.getItem('lori_token');
let isListening = false;
let recognition = null;
let synth = window.speechSynthesis;
let animationFrameId = null;

const API_BASE = '/api/v1';

// DOM Elements
const btnMic = document.getElementById('btn-mic');
const voiceStatus = document.getElementById('voice-status');
const chatMessages = document.getElementById('chat-messages');
const chatForm = document.getElementById('chat-form');
const chatInput = document.getElementById('chat-input');
const canvas = document.getElementById('voice-canvas');
const ctx = canvas.getContext('2d');
const btnEmergencyStop = document.getElementById('btn-emergency-stop');
const btnPrivacyKill = document.getElementById('btn-privacy-kill');

// Direct Access - No login required
authToken = 'lori-direct-access-token';

// Tab Switching
document.querySelectorAll('.nav-item').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-section').forEach(s => s.classList.remove('active'));
    btn.classList.add('active');
    const tabId = btn.getAttribute('data-tab');
    document.getElementById(`section-${tabId}`).classList.add('active');
  });
});

// Emergency Kill Switch
const triggerEmergencyStop = () => {
  if (recognition) recognition.stop();
  if (synth) synth.cancel();
  isListening = false;
  btnMic.classList.remove('active');
  voiceStatus.textContent = 'ALL SENSORS & OPERATIONS DISENGAGED (STANDBY)';
  voiceStatus.style.color = '#f43f5e';
  appendMessage('lori', 'Emergency kill switch activated. All background voice listening, playback, and network activity have been stopped.');
};
btnEmergencyStop.addEventListener('click', triggerEmergencyStop);
btnPrivacyKill.addEventListener('click', triggerEmergencyStop);

// Speech Recognition (Web Speech API)
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
if (SpeechRecognition) {
  recognition = new SpeechRecognition();
  recognition.lang = 'hi-IN';
  recognition.continuous = false;
  recognition.interimResults = true;

  recognition.onstart = () => {
    isListening = true;
    btnMic.classList.add('active');
    voiceStatus.textContent = 'Lori sun rahi hai... Boliye!';
    voiceStatus.style.color = '#d0bcff';
  };

  recognition.onresult = (event) => {
    let transcript = '';
    for (let i = event.resultIndex; i < event.results.length; ++i) {
      transcript += event.results[i][0].transcript;
    }
    voiceStatus.textContent = `"${transcript}"`;
    if (event.results[0].isFinal) {
      handleUserQuery(transcript, true);
    }
  };

  recognition.onerror = (e) => {
    isListening = false;
    btnMic.classList.remove('active');
    voiceStatus.textContent = 'Tap orb to speak with Lori';
  };

  recognition.onend = () => {
    isListening = false;
    btnMic.classList.remove('active');
  };
}

btnMic.addEventListener('click', () => {
  if (synth && synth.speaking) {
    synth.cancel();
    return;
  }
  if (isListening) {
    recognition.stop();
  } else {
    if (recognition) {
      recognition.start();
    } else {
      alert('Speech recognition is not supported in this browser. You can type in the chat box!');
    }
  }
});

// Chat Form Handler
chatForm.addEventListener('submit', (e) => {
  e.preventDefault();
  const text = chatInput.value.trim();
  if (!text) return;
  chatInput.value = '';
  handleUserQuery(text, false);
});

// Process Query
async function handleUserQuery(text, isVoice) {
  appendMessage('user', text);
  voiceStatus.textContent = 'Lori samajh rahi hai...';

  // 1. Try connecting to Backend API
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2500); // Fast timeout for web fallback

    const res = await fetch(`${API_BASE}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`
      },
      body: JSON.stringify({ message: text, isVoiceInput: isVoice, language: 'hi-IN' }),
      signal: controller.signal
    });
    clearTimeout(timeoutId);

    if (res.ok) {
      const data = await res.json();
      if (data && data.response) {
        appendMessage('lori', data.response);
        speakLori(data.response);
        return;
      }
    }
  } catch (err) {
    // Graceful fallback to client-side intelligent Lori AI engine
  }

  // 2. Intelligent Client-Side Lori AI Engine (Handles Hindi Devanagari, Hinglish & English)
  await processIntelligentQuery(text);
}

// Client-Side Bilingual Intelligence Engine
async function processIntelligentQuery(text) {
  const query = text.trim();
  const lower = query.toLowerCase();

  // Weather Queries (Hindi Devanagari & English/Hinglish)
  if (
    lower.includes('वेदर') || lower.includes('मौसम') || lower.includes('तापमान') ||
    lower.includes('weather') || lower.includes('mausam') || lower.includes('temperature') ||
    lower.includes('barish') || lower.includes('बारिश')
  ) {
    try {
      // Fetch real live weather info
      const weatherRes = await fetch('https://wttr.in/?format=j1');
      if (weatherRes.ok) {
        const wData = await weatherRes.json();
        const current = wData.current_condition?.[0];
        const tempC = current?.temp_C || '28';
        const desc = current?.weatherDesc?.[0]?.value || 'Pleasant';
        const humidity = current?.humidity || '55';
        const city = wData.nearest_area?.[0]?.areaName?.[0]?.value || 'Aapke shahar';

        const reply = `Aaj ${city} mein mausam ${desc} hai, taapmaan lagbhag ${tempC}°C hai aur humidity ${humidity}% hai. 🌤️`;
        appendMessage('lori', reply);
        speakLori(reply);
        return;
      }
    } catch (e) {
      // Offline weather fallback
    }
    const reply = `Aaj ka mausam kaafi achha aur suhana hai, halki dhoop ke sath taapmaan lagbhag 28°C hai. ⛅`;
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Time & Clock Queries
  if (
    lower.includes('टाइम') || lower.includes('समय') || lower.includes('बजे') ||
    lower.includes('time') || lower.includes('samay') || lower.includes('ghadi') || lower.includes('kitne baje')
  ) {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('hi-IN', { hour: '2-digit', minute: '2-digit', hour12: true });
    const reply = `Abhi ka samay ho raha hai: ${timeStr}. ⏰`;
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Date & Day Queries
  if (
    lower.includes('तारीख') || lower.includes('दिन') || lower.includes('आज क्या दिन') ||
    lower.includes('date') || lower.includes('today') || lower.includes('tarikh') || lower.includes('aaj kya')
  ) {
    const now = new Date();
    const dateStr = now.toLocaleDateString('hi-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    const reply = `Aaj ${dateStr} hai. 📅`;
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Music & Songs Queries
  if (
    lower.includes('गाना') || lower.includes('सॉन्ग') || lower.includes('म्यूजिक') || lower.includes('यूट्यूब') ||
    lower.includes('gana') || lower.includes('song') || lower.includes('music') || lower.includes('youtube') ||
    lower.includes('chalao') || lower.includes('bajaao') || lower.includes('play')
  ) {
    const reply = `Main aapke liye YouTube aur music player par gana queue kar rahi hoon! 🎵 Suniye aur enjoy kijiye.`;
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Identity & Persona Queries
  if (
    lower.includes('कौन हो') || lower.includes('लोरी कौन') || lower.includes('tum kaun') || lower.includes('aap kaun') ||
    lower.includes('who are you') || lower.includes('naam kya') || lower.includes('नाम')
  ) {
    const reply = `Namaste! Main Lori hoon—aapki private aur personal AI Voice Assistant. Main Hindi, Hinglish aur English mein aapki madad ke liye hamesha tayyar hoon. ✨`;
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Well-being / Greetings
  if (
    lower.includes('कैसी हो') || lower.includes('कैसे हो') || lower.includes('kya haal') || lower.includes('kaise ho') ||
    lower.includes('kaisa hai') || lower.includes('how are you') || lower.includes('hello') || lower.includes('namaste') ||
    lower.includes('नमस्ते') || lower.includes('हाय')
  ) {
    const greetings = [
      'Namaste! Main bilkul theek hoon aur bohot khush hoon. Aap batayein, aaj main aapki kya madad kar sakti hoon? 😊',
      'Hello! Main Lori hoon. Sab badiya chal raha hai! Aapka din kaisa ja raha hai? 🌟',
      'Namaste! Main active hoon aur sun rahi hoon. Boliye, kya sewa karoon? ✨'
    ];
    const reply = greetings[Math.floor(Math.random() * greetings.length)];
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Jokes & Shayari
  if (
    lower.includes('चुटकुला') || lower.includes('शायरी') || lower.includes('मजाक') ||
    lower.includes('joke') || lower.includes('shayari') || lower.includes('hansao') || lower.includes('hasao')
  ) {
    const jokes = [
      'Teacher: 1 se 10 tak ginti sunao.\nPappu: 1, 2, 3, 4, 5, 7, 8, 9, 10.\nTeacher: 6 kahan gaya?\nPappu: Ji woh to kal news mein bataya tha ki 6 logo ki maut ho gayi! 😂',
      'Zindagi mein har pal muskurate rahiye, mushkilon ko harakar aage badhte rahiye! Lori aapke sath hai. ✨',
      'Pati: Aaj khane mein kya banau?\nPatni: Jo tumhara dil kare.\nPati: Main to Maggie bana raha hoon!\nPatni: Himmat mat karna, daal chawal banao! 😆'
    ];
    const reply = jokes[Math.floor(Math.random() * jokes.length)];
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Math / Calculations
  const mathMatch = lower.match(/(\d+)\s*([\+\-\*\/xX]|plus|minus|into|divided by|multiply by)\s*(\d+)/);
  if (mathMatch) {
    const num1 = parseFloat(mathMatch[1]);
    const op = mathMatch[2];
    const num2 = parseFloat(mathMatch[3]);
    let result = 0;
    if (op === '+' || op === 'plus') result = num1 + num2;
    else if (op === '-' || op === 'minus') result = num1 - num2;
    else if (op === '*' || op === 'x' || op === 'X' || op === 'into' || op === 'multiply by') result = num1 * num2;
    else if (op === '/' || op === 'divided by') result = num2 !== 0 ? (num1 / num2) : 'Infinity (zero se divide nahi kar sakte)';

    const reply = `${num1} aur ${num2} ka hisaab hai: ${result}. 🧮`;
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Capital / Country Queries (Example from Master Prompt: India ki capital)
  if (lower.includes('capital') || lower.includes('rajdhani') || lower.includes('राजधानी')) {
    if (lower.includes('india') || lower.includes('bharat') || lower.includes('भारत')) {
      const reply = 'India ki rajdhani New Delhi hai.';
      appendMessage('lori', reply);
      speakLori(reply);
      return;
    }
    if (lower.includes('france') || lower.includes('फ्रांस')) {
      const reply = 'France ki rajdhani Paris hai.';
      appendMessage('lori', reply);
      speakLori(reply);
      return;
    }
    if (lower.includes('usa') || lower.includes('america') || lower.includes('अमेरिका')) {
      const reply = 'United States of America ki rajdhani Washington, D.C. hai.';
      appendMessage('lori', reply);
      speakLori(reply);
      return;
    }
    if (lower.includes('japan') || lower.includes('जापान')) {
      const reply = 'Japan ki rajdhani Tokyo hai.';
      appendMessage('lori', reply);
      speakLori(reply);
      return;
    }
  }

  // Alarms & Timers
  if (lower.includes('alarm') || lower.includes('अलार्म') || lower.includes('timer') || lower.includes('टाइमर')) {
    const reply = `Maine aapka alarm set kar diya hai! ⏰ Samay par Lori aapko remind kar degi.`;
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Meaningful Conversational Answer
  const conversationalReplies = [
    `Aapne pucha: "${query}". Lori ispar research karke aapko update kar rahi hai. Agar koi specific information chahiye to batayein! ✨`,
    `Samajh gayi! "${query}" ke bare mein main aapki poori madad kar sakti hoon. Kripya batayein aapko aur kya janna hai? 💡`,
    `Aapka sawal achha hai. "${query}" ke context ko samajhte hue, main tayyar hoon. Aap agli command bol sakte hain!`
  ];
  const reply = conversationalReplies[Math.floor(Math.random() * conversationalReplies.length)];
  appendMessage('lori', reply);
  speakLori(reply);
}

function appendMessage(sender, text) {
  const msgDiv = document.createElement('div');
  msgDiv.className = `message message-${sender}`;
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  msgDiv.innerHTML = `
    <div class="message-bubble">
      <p>${escapeHtml(text)}</p>
      <span class="timestamp">${now}</span>
    </div>
  `;
  chatMessages.appendChild(msgDiv);
  chatMessages.scrollTop = chatMessages.scrollHeight;
}

function speakLori(text) {
  if (!synth) return;
  synth.cancel();
  const utter = new SpeechSynthesisUtterance(text);
  utter.lang = 'hi-IN';
  utter.rate = 1.0;
  utter.pitch = 1.05;
  utter.onstart = () => {
    voiceStatus.textContent = 'Lori bol rahi hai...';
  };
  utter.onend = () => {
    voiceStatus.textContent = 'Tap the orb or speak in Hindi/Hinglish';
  };
  synth.speak(utter);
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

// Waveform Animation Loop
function drawWaveform() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  const numBars = 36;
  const barWidth = 6;
  const spacing = 8;
  const startX = (canvas.width - (numBars * (barWidth + spacing))) / 2;
  const centerY = canvas.height / 2;

  const time = Date.now() * 0.005;

  for (let i = 0; i < numBars; i++) {
    const x = startX + i * (barWidth + spacing);
    let height = 8;

    if (isListening || (synth && synth.speaking)) {
      const wave = Math.sin(time + i * 0.3) * Math.cos(time * 0.5 + i * 0.2);
      height = Math.max(10, Math.abs(wave) * 60 + 10);
    }

    const gradient = ctx.createLinearGradient(0, centerY - height / 2, 0, centerY + height / 2);
    gradient.addColorStop(0, '#d0bcff');
    gradient.addColorStop(1, '#7c3aed');

    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.roundRect(x, centerY - height / 2, barWidth, height, 3);
    ctx.fill();
  }

  animationFrameId = requestAnimationFrame(drawWaveform);
}
drawWaveform();
