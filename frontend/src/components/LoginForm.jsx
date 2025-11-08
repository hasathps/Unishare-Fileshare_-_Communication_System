import React, { useMemo, useState } from 'react';
import { LogIn, UserPlus, GraduationCap, ShieldCheck, Sparkles } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const LoginForm = () => {
  const { login, register, apiBaseUrl } = useAuth();
  const [mode, setMode] = useState('login'); // 'login' | 'signup'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const isSignup = mode === 'signup';
  const primaryIcon = isSignup ? <UserPlus size={26} /> : <LogIn size={26} />;
  const primaryCta = isSignup ? 'Create Account' : 'Sign in';
  const secondaryCta = isSignup ? 'Already have an account?' : "New here?";

  const resetForm = () => {
    setEmail('');
    setPassword('');
    setDisplayName('');
    setConfirmPassword('');
  };

  const toggleMode = () => {
    setMode((prev) => (prev === 'login' ? 'signup' : 'login'));
    resetForm();
  };

  const formValid = useMemo(() => {
    if (!email || !password) return false;
    if (isSignup && (!confirmPassword || confirmPassword !== password)) return false;
    return true;
  }, [email, password, confirmPassword, isSignup]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!formValid) return;

    setSubmitting(true);
    try {
      if (isSignup) {
        await register({ email, password, displayName });
      } else {
        await login(email, password);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen relative overflow-hidden bg-slate-950 flex items-center justify-center px-4 py-16">
      <div className="absolute inset-0 bg-gradient-to-br from-blue-500/20 via-slate-900/40 to-purple-500/20 pointer-events-none" />
      <div className="absolute -top-40 -left-24 h-72 w-72 rounded-full bg-blue-500/30 blur-3xl" />
      <div className="absolute -bottom-32 -right-16 h-80 w-80 rounded-full bg-purple-500/20 blur-3xl" />

      <div className="relative w-full max-w-5xl mx-auto backdrop-blur-xl rounded-3xl border border-white/10 bg-white/5 shadow-2xl">
        <div className="grid md:grid-cols-2">
          <div className="hidden md:flex flex-col justify-between border-r border-white/10 p-10 text-white">
            <div>
              <div className="flex items-center space-x-3">
                <div className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10">
                  <GraduationCap size={26} />
                </div>
                <div>
                  <h2 className="text-2xl font-semibold tracking-wide">UniShare</h2>
                  <p className="text-white/60 text-sm">University File Sharing Hub</p>
                </div>
              </div>

              <div className="mt-12 space-y-6">
                <div className="flex items-start space-x-4">
                  <div className="mt-1 rounded-full bg-white/10 p-2">
                    <Sparkles size={18} />
                  </div>
                  <div>
                    <h3 className="font-medium text-lg">Streamlined collaboration</h3>
                    <p className="text-sm text-white/70">
                      Share lecture notes, assignments, and resources in seconds with your classmates.
                    </p>
                  </div>
                </div>
                <div className="flex items-start space-x-4">
                  <div className="mt-1 rounded-full bg-white/10 p-2">
                    <ShieldCheck size={18} />
                  </div>
                  <div>
                    <h3 className="font-medium text-lg">Secure cloud storage</h3>
                    <p className="text-sm text-white/70">
                      Every upload is protected and tracked. Know exactly who shared what and when.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div className="space-y-3 text-white/70 text-sm">
              <p>API endpoint: {apiBaseUrl}</p>
              <p className="text-white/50">
                Need help? Contact your course coordinator for access &amp; support.
              </p>
            </div>
          </div>

          <div className="p-8 sm:p-10 bg-white/90 backdrop-blur-xl rounded-3xl md:rounded-l-none">
            <div className="flex items-center justify-between mb-8">
              <div className="inline-flex items-center space-x-3 text-blue-600">
                <span className="inline-flex items-center justify-center rounded-2xl bg-blue-100 h-12 w-12 shadow-inner">
                  {primaryIcon}
                </span>
                <div>
                  <h1 className="text-xl font-semibold text-slate-900">
                    {isSignup ? 'Create your UniShare account' : 'Welcome back'}
                  </h1>
                  <p className="text-sm text-slate-500">
                    {isSignup ? 'Join your classmates in seconds.' : 'Sign in to continue sharing knowledge.'}
                  </p>
                </div>
              </div>
              <button
                onClick={toggleMode}
                className="text-sm font-medium text-blue-600 hover:text-blue-700 transition"
              >
                {secondaryCta}
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-600">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  autoComplete="email"
                  required
                  className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
                  placeholder="name@university.edu"
                />
              </div>

              {isSignup && (
                <div className="space-y-2">
                  <label className="text-sm font-medium text-slate-600">Display name</label>
                  <input
                    type="text"
                    value={displayName}
                    onChange={(event) => setDisplayName(event.target.value)}
                    placeholder="How should others see you?"
                    className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
                  />
                </div>
              )}

              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-600">Password</label>
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  autoComplete={isSignup ? 'new-password' : 'current-password'}
                  required
                  className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
                  placeholder="••••••••"
                />
              </div>

              {isSignup && (
                <div className="space-y-2">
                  <label className="text-sm font-medium text-slate-600">Confirm password</label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    autoComplete="new-password"
                    required
                    className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
                    placeholder="Re-enter password"
                  />
                  {confirmPassword && confirmPassword !== password && (
                    <p className="text-xs text-red-500">Passwords do not match.</p>
                  )}
                </div>
              )}

              <button
                type="submit"
                disabled={submitting || !formValid}
                className={`w-full rounded-xl px-4 py-3 text-white font-semibold shadow-lg shadow-blue-500/30 transition transform ${
                  submitting || !formValid
                    ? 'bg-blue-300 cursor-not-allowed'
                    : 'bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 hover:-translate-y-0.5'
                }`}
              >
                {submitting ? (isSignup ? 'Creating account…' : 'Signing in…') : primaryCta}
              </button>
            </form>

            <div className="mt-10 grid grid-cols-2 gap-4 text-xs text-slate-500">
              <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
                <p className="font-semibold text-slate-700">Track uploads</p>
                <p className="mt-1 leading-tight">
                  Every file records who uploaded it. Stay accountable and keep your workspace organized.
                </p>
              </div>
              <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
                <p className="font-semibold text-slate-700">Instant access</p>
                <p className="mt-1 leading-tight">
                  Log in from any device to pick up where you left off—your resources live in the cloud.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginForm;

