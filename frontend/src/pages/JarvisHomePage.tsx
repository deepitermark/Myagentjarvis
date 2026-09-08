import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import {
  Activity, ArrowRight, Brain, Check, ChevronRight, Clock3, Command,
  Database, Ghost, Mic, Moon, Phone, Play, Plus, Search, Settings2,
  ShieldCheck, Sparkles, SquareTerminal, Sun, Volume2, WandSparkles, Zap,
} from 'lucide-react';
import { useAppStore } from '../lib/store';

type SpeechRecognitionLike = {
  lang: string; continuous: boolean; interimResults: boolean;
  start: () => void; stop: () => void;
  onresult: ((event: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null;
  onerror: (() => void) | null; onend: (() => void) | null;
};

declare global { interface Window { SpeechRecognition?: new () => SpeechRecognitionLike; webkitSpeechRecognition?: new () => SpeechRecognitionLike } }

const quickActions = [
  { label: 'Ghost Call', detail: 'Practice an AI-handled call', icon: Phone, route: '/agents', tone: 'violet' },
  { label: 'Screen Automate', detail: 'Build a safe routine', icon: SquareTerminal, route: '/agents', tone: 'teal' },
  { label: 'Memory', detail: 'Search your private context', icon: Brain, route: '/dashboard', tone: 'amber' },
  { label: 'Translate', detail: '18-language workspace', icon: WandSparkles, route: '/chat', tone: 'pink' },
  { label: 'Privacy', detail: 'Review local data controls', icon: ShieldCheck, route: '/settings', tone: 'green' },
  { label: 'Voice Center', detail: 'Tune speech & profiles', icon: Volume2, route: '/settings', tone: 'blue' },
];

const suggestions = [
  'Summarize my recent activity',
  'Create a focused 25 minute work session',
  'Explain what is running locally',
];

export function JarvisHomePage() {
  const navigate = useNavigate();
  const settings = useAppStore((s) => s.settings);
  const updateSettings = useAppStore((s) => s.updateSettings);
  const serverInfo = useAppStore((s) => s.serverInfo);
  const [listening, setListening] = useState(false);
  const [command, setCommand] = useState('');
  const recognition = useRef<SpeechRecognitionLike | null>(null);
  const [focusActive, setFocusActive] = useState(false);

  useEffect(() => () => recognition.current?.stop(), []);

  const startVoice = () => {
    const Recognition = window.SpeechRecognition ?? window.webkitSpeechRecognition;
    if (!Recognition) {
      toast.info('Voice capture is not available in this browser. Use the chat mic when the local voice engine is enabled.');
      navigate('/chat');
      return;
    }
    const instance = new Recognition();
    recognition.current = instance;
    instance.lang = 'hi-IN'; instance.continuous = false; instance.interimResults = true;
    instance.onresult = (event) => setCommand(Array.from(event.results).map((r) => r[0].transcript).join(''));
    instance.onerror = () => { setListening(false); toast.error('Mic permission ya voice capture fail hua.'); };
    instance.onend = () => setListening(false);
    setListening(true); instance.start();
  };

  const sendCommand = () => {
    if (!command.trim()) { toast.info('Pehle command boliye ya type kijiye.'); return; }
    localStorage.setItem('oj-pending-command', command.trim());
    toast.success('Command chat mein ready hai');
    navigate('/chat');
  };

  const toggleTheme = () => updateSettings({ theme: settings.theme === 'dark' ? 'light' : 'dark' });

  return (
    <div className="jarvis-home h-full overflow-y-auto">
      <div className="jarvis-shell">
        <header className="jarvis-topbar">
          <div className="brand-lockup"><div className="brand-orb"><Sparkles size={19} /></div><div><p className="eyebrow">ULTIMATE AI ASSISTANT</p><h1>J.A.R.V.I.S <span>v3</span></h1></div></div>
          <div className="top-actions"><button className="icon-button" onClick={toggleTheme} aria-label="Toggle theme">{settings.theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}</button><button className="profile-chip" onClick={() => navigate('/settings')}><span className="status-dot" /> <span>Private mode</span><ChevronRight size={15} /></button></div>
        </header>

        <section className="hero-grid">
          <div className="hero-copy"><div className="live-pill"><span className="pulse-dot" /> {serverInfo ? 'LOCAL CORE ONLINE' : 'READY FOR COMMANDS'}</div><h2>Namaste.<br /><em>What shall we</em> <strong>unlock?</strong></h2><p>One calm command center for chat, memory, voice and safe automation. Your data stays in your control.</p><div className="hero-actions"><button className="primary-action" onClick={() => navigate('/chat')}><Command size={17} /> Open full chat <ArrowRight size={16} /></button><button className="quiet-action" onClick={() => navigate('/get-started')}><Play size={15} /> Take the tour</button></div></div>
          <div className="voice-console"><div className="console-top"><span>VOICE CONSOLE</span><span className={listening ? 'console-state listening-state' : 'console-state'}>{listening ? 'LISTENING' : 'STANDBY'}</span></div><button className={`voice-orb ${listening ? 'is-listening' : ''}`} onClick={listening ? () => recognition.current?.stop() : startVoice} aria-label={listening ? 'Stop listening' : 'Start voice command'}><div className="orb-ring ring-one" /><div className="orb-ring ring-two" />{listening ? <Activity size={31} /> : <Mic size={31} />}<span>{listening ? 'Tap to stop' : 'Tap to speak'}</span></button><div className="wave-bars">{[1, 2, 3, 4, 5, 6, 7, 8, 9].map((n) => <i key={n} style={{ animationDelay: `${n * 80}ms` }} />)}</div><div className="command-row"><Search size={16} /><input value={command} onChange={(e) => setCommand(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && sendCommand()} placeholder="Type a command..." /><button onClick={sendCommand}><ArrowRight size={17} /></button></div></div>
        </section>

        <section className="status-strip"><div><span className="status-dot" /> <b>System active</b><small>All core services ready</small></div><div><Brain size={16} /><b>Local + cloud</b><small>Smart routing</small></div><div><ShieldCheck size={16} /><b>Privacy first</b><small>No silent sharing</small></div><div><Activity size={16} /><b>Low power</b><small>Balanced mode</small></div></section>

        <section className="section-block"><div className="section-heading"><div><p className="eyebrow">COMMAND DECK</p><h3>Make it happen</h3></div><button className="text-action" onClick={() => navigate('/agents')}>View all <ArrowRight size={15} /></button></div><div className="quick-grid">{quickActions.map(({ label, detail, icon: Icon, route, tone }) => <button key={label} className={`quick-card tone-${tone}`} onClick={() => navigate(route)}><span className="quick-icon"><Icon size={20} /></span><span><b>{label}</b><small>{detail}</small></span><ChevronRight size={16} className="quick-chevron" /></button>)}</div></section>

        <section className="lower-grid"><div className="panel-card"><div className="panel-heading"><div><p className="eyebrow">SMART SUGGESTIONS</p><h3>Ready when you are</h3></div><Sparkles size={19} /></div>{suggestions.map((item) => <button key={item} className="suggestion-row" onClick={() => { setCommand(item); toast.success('Command ready — send it when you want'); }}><span className="suggestion-bullet">✦</span><span>{item}</span><ArrowRight size={15} /></button>)}</div><div className="panel-card focus-card"><div className="panel-heading"><div><p className="eyebrow">FOCUS MODE</p><h3>{focusActive ? 'Session in progress' : 'Protect your attention'}</h3></div><Clock3 size={19} /></div><p>{focusActive ? 'Jarvis will keep distractions quiet for this session.' : 'Start a gentle 25-minute session with only essential actions.'}</p><button className={focusActive ? 'secondary-action' : 'primary-action'} onClick={() => { setFocusActive(!focusActive); toast.success(focusActive ? 'Focus mode ended' : 'Focus mode started'); }}>{focusActive ? 'End session' : 'Start focus'} <ArrowRight size={15} /></button></div></section>

        <footer className="jarvis-footer"><span><Database size={14} /> Memory synced locally</span><button onClick={() => navigate('/settings')}><Settings2 size={14} /> Configure assistant</button><button onClick={() => navigate('/dashboard')}><Zap size={14} /> See live telemetry</button></footer>
      </div>
    </div>
  );
}

export default JarvisHomePage;

// Keep these imports available to bundlers that tree-shake icon sets differently across Tauri/web builds.
void Ghost; void Plus;
