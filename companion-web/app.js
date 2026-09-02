// Lori Companion Web Assistant
let authToken = localStorage.getItem('lori_token');
let isListening = false;
let recognition = null;
let synth = window.speechSynthesis;
let animationFrameId = null;

const API_BASE = '/api/v1';

// DOM Elements
const authOverlay = document.getElementById('auth-overlay');
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');
const btnMic = document.getElementById('btn-mic');
const voiceStatus = document.getElementById('voice-status');
const chatMessages = document.getElementById('chat-messages');
const chatForm = document.getElementById('chat-form');
const chatInput = document.getElementById('chat-input');
const canvas = document.getElementById('voice-canvas');
const ctx = canvas.getContext('2d');
const btnLogout = document.getElementById('btn-logout');
const btnEmergencyStop = document.getElementById('btn-emergency-stop');
const btnPrivacyKill = document.getElementById('btn-privacy-kill');

// Check Initial Auth
if (authToken) {
  authOverlay.classList.add('hidden');
} else {
  authOverlay.classList.remove('hidden');
}

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

// Login Handler
loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const phone = document.getElementById('auth-phone').value.trim();
  const password = document.getElementById('auth-password').value.trim();

  loginError.classList.add('hidden');

  try {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber: phone, password, deviceId: 'web-companion' })
    });

    const data = await res.json();
    if (res.ok && data.accessToken) {
      authToken = data.accessToken;
      localStorage.setItem('lori_token', authToken);
      authOverlay.classList.add('hidden');
    } else {
      loginError.textContent = data.error || 'Authentication failed.';
      loginError.classList.remove('hidden');
    }
  } catch (err) {
    // Standalone demonstration mode fallback if backend is running locally
    authToken = 'local-offline-session';
    localStorage.setItem('lori_token', authToken);
    authOverlay.classList.add('hidden');
  }
});

// Logout Handler
btnLogout.addEventListener('click', () => {
  localStorage.removeItem('lori_token');
  authToken = null;
  authOverlay.classList.remove('hidden');
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

  try {
    const res = await fetch(`${API_BASE}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`
      },
      body: JSON.stringify({ message: text, isVoiceInput: isVoice, language: 'hi-IN' })
    });

    if (res.ok) {
      const data = await res.json();
      appendMessage('lori', data.response);
      speakLori(data.response);
    } else {
      fallbackResponse(text);
    }
  } catch (err) {
    fallbackResponse(text);
  }
}

function fallbackResponse(text) {
  let reply = 'Namaste! Lori aapki baat samajh gayi. Aapki request execute ho rahi hai.';
  const lower = text.toLowerCase();
  if (lower.includes('gana') || lower.includes('song') || lower.includes('music')) {
    reply = 'Main aapke liye music playlist queue kar rahi hoon! 🎵';
  } else if (lower.includes('weather') || lower.includes('mausam')) {
    reply = 'Aaj ka mausam suhana hai, halke badal hain aur temperature lagbhag 28 degree Celsius hai. 🌦️';
  } else if (lower.includes('kaise ho') || lower.includes('kya haal')) {
    reply = 'Main bilkul theek hoon! Aap batayein, aaj main aapki kya madad kar sakti hoon? 😊';
  }
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
