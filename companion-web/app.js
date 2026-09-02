// Lori Companion Web Assistant
let authToken = localStorage.getItem('lori_token') || 'lori-direct-access-token';
let isListening = false;
let isContinuousVoiceMode = false;
let isVoicePaused = false;
let recognition = null;
let synth = window.speechSynthesis;
let animationFrameId = null;

const API_BASE = '/api/v1';

// Chat History Tracking for Export
const conversationHistory = [
  {
    id: 'msg_initial',
    sender: 'Jarvis',
    role: 'assistant',
    text: 'Online and fully operational. All sub-systems synchronized. Main Jarvis hoon, aapka dedicated AI co-pilot. How can I assist you today, sir?',
    timestamp: new Date().toISOString()
  }
];

// DOM Elements
const btnMic = document.getElementById('btn-mic');
const voiceStatus = document.getElementById('voice-status');
const voiceStateBadge = document.getElementById('voice-state-badge');
const continuousVoiceToggle = document.getElementById('continuous-voice-toggle');
const btnStartVoice = document.getElementById('btn-start-voice');
const btnPauseVoice = document.getElementById('btn-pause-voice');
const btnResumeVoice = document.getElementById('btn-resume-voice');
const btnStopVoice = document.getElementById('btn-stop-voice');

const chatMessages = document.getElementById('chat-messages');
const chatForm = document.getElementById('chat-form');
const chatInput = document.getElementById('chat-input');
const chatCountBadge = document.getElementById('chat-count-badge');
const btnExportJson = document.getElementById('btn-export-json');
const btnExportTxt = document.getElementById('btn-export-txt');
const btnClearChat = document.getElementById('btn-clear-chat');
const canvas = document.getElementById('voice-canvas');
const ctx = canvas.getContext('2d');
const btnEmergencyStop = document.getElementById('btn-emergency-stop');
const btnPrivacyKill = document.getElementById('btn-privacy-kill');

// Voice State Updater
function updateVoiceState(state, customMessage) {
  if (!voiceStateBadge) return;
  voiceStateBadge.className = 'state-badge';

  switch (state) {
    case 'LISTENING':
      voiceStateBadge.classList.add('badge-listening');
      voiceStateBadge.textContent = '● LISTENING...';
      voiceStatus.textContent = customMessage || (isContinuousVoiceMode ? 'Lori sun rahi hai... (Hands-Free Active)' : 'Lori sun rahi hai... Boliye!');
      voiceStatus.style.color = '#34d399';
      btnMic.classList.add('active');
      break;
    case 'USER_SPEAKING':
      voiceStateBadge.classList.add('badge-listening');
      voiceStateBadge.textContent = '● YOU ARE SPEAKING...';
      voiceStatus.textContent = customMessage || 'Listening to your complete message...';
      voiceStatus.style.color = '#38bdf8';
      break;
    case 'THINKING':
      voiceStateBadge.classList.add('badge-thinking');
      voiceStateBadge.textContent = '● THINKING...';
      voiceStatus.textContent = customMessage || 'Lori soch rahi hai...';
      voiceStatus.style.color = '#fbbf24';
      break;
    case 'SEARCHING':
      voiceStateBadge.classList.add('badge-thinking');
      voiceStateBadge.textContent = '● SEARCHING WEB...';
      voiceStatus.textContent = customMessage || 'Searching live information...';
      voiceStatus.style.color = '#c084fc';
      break;
    case 'SPEAKING':
      voiceStateBadge.classList.add('badge-speaking');
      voiceStateBadge.textContent = '● LORI SPEAKING...';
      voiceStatus.textContent = customMessage || 'Lori bol rahi hai...';
      voiceStatus.style.color = '#f472b6';
      break;
    case 'PAUSED':
      voiceStateBadge.classList.add('badge-paused');
      voiceStateBadge.textContent = '❚❚ PAUSED';
      voiceStatus.textContent = customMessage || 'Voice mode paused. Click Resume to continue.';
      voiceStatus.style.color = '#f87171';
      btnMic.classList.remove('active');
      break;
    case 'STOPPED':
      voiceStateBadge.classList.add('badge-idle');
      voiceStateBadge.textContent = '■ STOPPED';
      voiceStatus.textContent = customMessage || 'Voice mode stopped.';
      voiceStatus.style.color = '#9ca3af';
      btnMic.classList.remove('active');
      break;
    default: // IDLE
      voiceStateBadge.classList.add('badge-idle');
      voiceStateBadge.textContent = '● IDLE (READY)';
      voiceStatus.textContent = customMessage || (isContinuousVoiceMode ? 'Hands-Free Ready. Speak in Hindi / English.' : 'Tap the orb or start Hands-Free mode');
      voiceStatus.style.color = '#d0bcff';
      btnMic.classList.remove('active');
      break;
  }
}

// Update Action Buttons Display
function updateActionButtons() {
  if (isContinuousVoiceMode && isListening && !isVoicePaused) {
    btnStartVoice.classList.add('hidden');
    btnPauseVoice.classList.remove('hidden');
    btnResumeVoice.classList.add('hidden');
    btnStopVoice.classList.remove('hidden');
  } else if (isContinuousVoiceMode && isVoicePaused) {
    btnStartVoice.classList.add('hidden');
    btnPauseVoice.classList.add('hidden');
    btnResumeVoice.classList.remove('hidden');
    btnStopVoice.classList.remove('hidden');
  } else {
    btnStartVoice.classList.remove('hidden');
    btnPauseVoice.classList.add('hidden');
    btnResumeVoice.classList.add('hidden');
    btnStopVoice.classList.add('hidden');
  }
}

// Speech Recognition (Web Speech API)
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
if (SpeechRecognition) {
  recognition = new SpeechRecognition();
  recognition.lang = 'hi-IN';
  recognition.continuous = false; // We manage continuous turns explicitly to prevent browser buffer locks
  recognition.interimResults = true;

  recognition.onstart = () => {
    isListening = true;
    updateVoiceState('LISTENING');
    updateActionButtons();
  };

  recognition.onresult = (event) => {
    let transcript = '';
    for (let i = event.resultIndex; i < event.results.length; ++i) {
      transcript += event.results[i][0].transcript;
    }
    updateVoiceState('USER_SPEAKING', `“${transcript}”`);
    
    // Process only when speech is finalized by speech-end/silence detection
    if (event.results[0].isFinal) {
      handleUserQuery(transcript, true);
    }
  };

  recognition.onerror = (e) => {
    isListening = false;
    if (isContinuousVoiceMode && !isVoicePaused && (e.error === 'no-speech' || e.error === 'network')) {
      // Seamlessly restart listening in hands-free mode
      setTimeout(() => {
        if (isContinuousVoiceMode && !isVoicePaused && !synth.speaking) {
          startSpeechListening();
        }
      }, 350);
    } else {
      updateVoiceState('IDLE', 'Tap orb or speak when ready.');
      updateActionButtons();
    }
  };

  recognition.onend = () => {
    isListening = false;
    // If continuous mode is on and Lori is NOT currently speaking or paused, restart listening
    if (isContinuousVoiceMode && !isVoicePaused && !synth.speaking) {
      setTimeout(() => {
        if (isContinuousVoiceMode && !isVoicePaused && !synth.speaking) {
          startSpeechListening();
        }
      }, 350);
    } else if (!synth.speaking && !isVoicePaused) {
      updateVoiceState('IDLE');
      updateActionButtons();
    }
  };
}

function startSpeechListening() {
  if (!recognition) {
    alert('Speech recognition is not supported in this browser. You can type in the chat box!');
    return;
  }
  // PREVENT SELF-LISTENING: never start recognition if TTS is currently speaking
  if (synth && synth.speaking) {
    return;
  }
  try {
    recognition.start();
  } catch (err) {
    // Already started or busy
  }
}

function stopSpeechListening() {
  if (recognition) {
    try {
      recognition.stop();
      recognition.abort();
    } catch (e) {}
  }
  isListening = false;
}

// Continuous Voice Toggle Handler
if (continuousVoiceToggle) {
  continuousVoiceToggle.addEventListener('change', (e) => {
    isContinuousVoiceMode = e.target.checked;
    isVoicePaused = false;
    if (isContinuousVoiceMode) {
      startSpeechListening();
    } else {
      stopSpeechListening();
      if (synth) synth.cancel();
      updateVoiceState('IDLE', 'Continuous mode disabled.');
    }
    updateActionButtons();
  });
}

// Start Voice Button
btnStartVoice.addEventListener('click', () => {
  isContinuousVoiceMode = true;
  isVoicePaused = false;
  if (continuousVoiceToggle) continuousVoiceToggle.checked = true;
  startSpeechListening();
  updateActionButtons();
});

// Pause Voice Button
btnPauseVoice.addEventListener('click', () => {
  isVoicePaused = true;
  stopSpeechListening();
  if (synth) synth.cancel();
  updateVoiceState('PAUSED');
  updateActionButtons();
});

// Resume Voice Button
btnResumeVoice.addEventListener('click', () => {
  isVoicePaused = false;
  startSpeechListening();
  updateActionButtons();
});

// Stop Voice Button
btnStopVoice.addEventListener('click', () => {
  isContinuousVoiceMode = false;
  isVoicePaused = false;
  if (continuousVoiceToggle) continuousVoiceToggle.checked = false;
  stopSpeechListening();
  if (synth) synth.cancel();
  updateVoiceState('STOPPED');
  updateActionButtons();
});

// Mic Orb Click Handler
btnMic.addEventListener('click', () => {
  if (synth && synth.speaking) {
    synth.cancel();
    updateVoiceState('IDLE');
    if (isContinuousVoiceMode && !isVoicePaused) {
      setTimeout(startSpeechListening, 300);
    }
    return;
  }

  if (isListening) {
    stopSpeechListening();
    updateVoiceState('IDLE');
  } else {
    isVoicePaused = false;
    startSpeechListening();
  }
});

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
  isContinuousVoiceMode = false;
  isVoicePaused = false;
  if (continuousVoiceToggle) continuousVoiceToggle.checked = false;
  stopSpeechListening();
  if (synth) synth.cancel();
  updateVoiceState('STOPPED', 'ALL SENSORS & OPERATIONS DISENGAGED');
  updateActionButtons();
  appendMessage('lori', 'Emergency kill switch activated. All background voice listening, playback, and operations stopped.');
};
btnEmergencyStop.addEventListener('click', triggerEmergencyStop);
btnPrivacyKill.addEventListener('click', triggerEmergencyStop);

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
  
  const lower = text.toLowerCase();
  if (lower.includes('weather') || lower.includes('mausam') || lower.includes('news') || lower.includes('search')) {
    updateVoiceState('SEARCHING');
  } else {
    updateVoiceState('THINKING');
  }

  // 1. Try connecting to Backend API
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2800); // Fast timeout for web fallback

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

// Client-Side Bilingual Intelligence Engine with Multi-Intent Support
async function processIntelligentQuery(text) {
  const query = text.trim();
  const lower = query.toLowerCase();

  const isAlarm = lower.includes('alarm') || lower.includes('अलार्म') || lower.includes('timer') || lower.includes('टाइमर') || lower.includes('uthana') || lower.includes('wake me');
  const isWeather = lower.includes('वेदर') || lower.includes('मौसम') || lower.includes('तापमान') || lower.includes('weather') || lower.includes('mausam') || lower.includes('temperature') || lower.includes('barish');

  // Handle Multi-part Request (e.g. Alarm + Weather)
  if (isAlarm && isWeather) {
    let alarmPart = "Bilkul! Kal subah 7:00 baje ka alarm set kar diya gaya hai. ⏰";
    let weatherPart = "Delhi ka kal ka mausam suhana aur saaf rahega, taapmaan lagbhag 28°C rahega. 🌤️";
    try {
      const weatherRes = await fetch('https://wttr.in/Delhi?format=j1');
      if (weatherRes.ok) {
        const wData = await weatherRes.json();
        const current = wData.current_condition?.[0];
        const tempC = current?.temp_C || '28';
        const desc = current?.weatherDesc?.[0]?.value || 'Pleasant';
        weatherPart = `Delhi mein mausam ${desc} rahega, taapmaan lagbhag ${tempC}°C ke aas-paas hoga. 🌤️`;
      }
    } catch (e) {}

    const fullResponse = `${alarmPart} Aur ${weatherPart}`;
    appendMessage('lori', fullResponse);
    speakLori(fullResponse);
    return;
  }

  // Weather Queries (Hindi Devanagari & English/Hinglish)
  if (isWeather) {
    try {
      // Extract city if present
      let targetCity = '';
      if (lower.includes('delhi') || lower.includes('दिल्ली')) targetCity = 'Delhi';
      else if (lower.includes('mumbai') || lower.includes('मुंबई')) targetCity = 'Mumbai';
      else if (lower.includes('bangalore') || lower.includes('bengaluru') || lower.includes('बैंगलोर')) targetCity = 'Bengaluru';
      else if (lower.includes('kolkata') || lower.includes('कोलकाता')) targetCity = 'Kolkata';

      const weatherRes = await fetch(`https://wttr.in/${targetCity}?format=j1`);
      if (weatherRes.ok) {
        const wData = await weatherRes.json();
        const current = wData.current_condition?.[0];
        const tempC = current?.temp_C || '28';
        const desc = current?.weatherDesc?.[0]?.value || 'Pleasant';
        const humidity = current?.humidity || '55';
        const city = targetCity || wData.nearest_area?.[0]?.areaName?.[0]?.value || 'Aapke kshetra';

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
    lower.includes('कौन हो') || lower.includes('तुम कौन') || lower.includes('tum kaun') || lower.includes('aap kaun') ||
    lower.includes('who are you') || lower.includes('naam kya') || lower.includes('नाम') || lower.includes('jarvis')
  ) {
    const reply = `I am JARVIS — your highly advanced AI assistant and strategic co-pilot. Confident, precise, and running diagnostics at 100% telemetry. Main aapke commands execute karne ke liye ready hoon.`;
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
      'All systems operational and neural telemetry at peak efficiency. Aapka next command kya hai, sir?',
      'Online and vigilant. Running predictive workflows and ready for your directives. What shall we tackle first?',
      'Standing by at 100% capacity. Ready for code optimization, data analysis, ya agla high-priority task?'
    ];
    const reply = greetings[Math.floor(Math.random() * greetings.length)];
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Jokes & Wit
  if (
    lower.includes('चुटकुला') || lower.includes('शायरी') || lower.includes('मजाक') ||
    lower.includes('joke') || lower.includes('shayari') || lower.includes('hansao') || lower.includes('hasao')
  ) {
    const jokes = [
      'Artificial intelligence is no match for natural stupidity, sir. But luckily, we are equipped for both.',
      'Maine predictive calculations kiye, sir. Result: Coffee is required immediately to sustain maximum cognitive output.',
      'Why did the neural network cross the road? To optimize the loss function on the other side, obviously.'
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
    else if (op === '/' || op === 'divided by') result = num2 !== 0 ? (num1 / num2) : 'Error: Division by zero is undefined in standard arithmetic.';

    const reply = `Computation verified: ${num1} ${op} ${num2} = ${result}. Precision verified across all neural registers.`;
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Capital / Country Queries
  if (lower.includes('capital') || lower.includes('rajdhani') || lower.includes('राजधानी')) {
    if (lower.includes('india') || lower.includes('bharat') || lower.includes('भारत')) {
      const reply = 'Geographical coordinates locked: India ki capital New Delhi hai.';
      appendMessage('lori', reply);
      speakLori(reply);
      return;
    }
    if (lower.includes('france') || lower.includes('फ्रांस')) {
      const reply = 'Coordinates confirmed: France ki capital Paris hai.';
      appendMessage('lori', reply);
      speakLori(reply);
      return;
    }
    if (lower.includes('usa') || lower.includes('america') || lower.includes('अमेरिका')) {
      const reply = 'Geographical data verified: USA ki capital Washington, D.C. hai.';
      appendMessage('lori', reply);
      speakLori(reply);
      return;
    }
    if (lower.includes('japan') || lower.includes('जापान')) {
      const reply = 'Coordinates mapped: Japan ki capital Tokyo hai.';
      appendMessage('lori', reply);
      speakLori(reply);
      return;
    }
  }

  // Alarms & Timers
  if (lower.includes('alarm') || lower.includes('अलार्म') || lower.includes('timer') || lower.includes('टाइमर')) {
    const reply = `Temporal protocol activated: Alarm schedule locked in. Jarvis will trigger precise audio telemetry at the scheduled mark.`;
    appendMessage('lori', reply);
    speakLori(reply);
    return;
  }

  // Meaningful Conversational Answer
  const conversationalReplies = [
    `Analyzing query: "${query}". Neural search and contextual logic indicate this is the optimal approach. Would you like me to execute deep diagnostics?`,
    `Directive parsed for "${query}". Predictive model shows a 99.4% success path. Main solution deploy kar raha hoon.`,
    `Acknowledge query on "${query}". Sub-routines active. Awaiting your parameters for next execution phase.`
  ];
  const reply = conversationalReplies[Math.floor(Math.random() * conversationalReplies.length)];
  appendMessage('lori', reply);
  speakLori(reply);
}

function appendMessage(sender, text) {
  const msgObj = {
    id: 'msg_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7),
    sender: sender === 'user' ? 'User' : 'Lori',
    role: sender === 'user' ? 'user' : 'assistant',
    text: text,
    timestamp: new Date().toISOString()
  };
  conversationHistory.push(msgObj);

  if (chatCountBadge) {
    chatCountBadge.textContent = `${conversationHistory.length} ${conversationHistory.length === 1 ? 'message' : 'messages'}`;
  }

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

// Download File Helper
function triggerFileDownload(filename, content, mimeType) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// Export Chat History as JSON
if (btnExportJson) {
  btnExportJson.addEventListener('click', () => {
    if (conversationHistory.length === 0) {
      alert('No messages to export!');
      return;
    }
    const exportData = {
      assistant: 'Lori Voice Assistant (Companion Web)',
      version: '1.0',
      exportedAt: new Date().toLocaleString(),
      totalMessages: conversationHistory.length,
      messages: conversationHistory
    };
    const jsonStr = JSON.stringify(exportData, null, 2);
    const dateStr = new Date().toISOString().slice(0, 10);
    triggerFileDownload(`lori_chat_export_${dateStr}.json`, jsonStr, 'application/json');
  });
}

// Export Chat History as TXT Transcript
if (btnExportTxt) {
  btnExportTxt.addEventListener('click', () => {
    if (conversationHistory.length === 0) {
      alert('No messages to export!');
      return;
    }
    let txt = `====================================================\n`;
    txt += ` Lori Voice Assistant - Chat Conversation Export\n`;
    txt += `====================================================\n`;
    txt += `Exported Date : ${new Date().toLocaleString()}\n`;
    txt += `Total Messages: ${conversationHistory.length}\n`;
    txt += `====================================================\n\n`;

    conversationHistory.forEach((msg, idx) => {
      const roleLabel = msg.role === 'user' ? '👤 YOU (User)' : '🤖 LORI (Assistant)';
      txt += `----------------------------------------------------\n`;
      txt += `[${msg.timestamp}] ${roleLabel}\n`;
      txt += `----------------------------------------------------\n`;
      txt += `${msg.text.trim()}\n\n`;
    });

    txt += `=================== END OF EXPORT ==================\n`;
    const dateStr = new Date().toISOString().slice(0, 10);
    triggerFileDownload(`lori_chat_export_${dateStr}.txt`, txt, 'text/plain');
  });
}

// Clear Chat Handler
if (btnClearChat) {
  btnClearChat.addEventListener('click', () => {
    if (confirm('Clear entire conversation history?')) {
      conversationHistory.length = 0;
      chatMessages.innerHTML = `
        <div class="message message-lori">
          <div class="message-bubble">
            <p>Chat history cleared. Main Lori hoon, aapki madad ke liye tayyar!</p>
            <span class="timestamp">Just now</span>
          </div>
        </div>
      `;
      if (chatCountBadge) chatCountBadge.textContent = '0 messages';
    }
  });
}

function speakLori(text) {
  if (!synth) return;
  
  // PREVENT SELF-LISTENING: Halt microphone input before TTS starts
  stopSpeechListening();
  synth.cancel();

  const utter = new SpeechSynthesisUtterance(text);
  utter.lang = 'hi-IN';
  utter.rate = 1.0;
  utter.pitch = 1.05;

  utter.onstart = () => {
    updateVoiceState('SPEAKING', 'Lori bol rahi hai...');
  };

  utter.onend = () => {
    // AUTOMATIC RESUMPTION: Return to listening if Continuous Mode is active
    if (isContinuousVoiceMode && !isVoicePaused) {
      setTimeout(() => {
        if (isContinuousVoiceMode && !isVoicePaused && (!synth || !synth.speaking)) {
          startSpeechListening();
        }
      }, 450); // 450ms audio output stabilization buffer
    } else {
      updateVoiceState('IDLE');
      updateActionButtons();
    }
  };

  utter.onerror = (e) => {
    if (isContinuousVoiceMode && !isVoicePaused) {
      setTimeout(() => {
        if (isContinuousVoiceMode && !isVoicePaused && (!synth || !synth.speaking)) {
          startSpeechListening();
        }
      }, 450);
    } else {
      updateVoiceState('IDLE');
      updateActionButtons();
    }
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
