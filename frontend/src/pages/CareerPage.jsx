import React, { useState, useEffect } from 'react';
import { 
  Briefcase, 
  Sparkles, 
  CheckCircle2, 
  AlertTriangle, 
  Brain, 
  Map, 
  Plus, 
  Trash2, 
  Edit3, 
  Target, 
  Layers, 
  Award,
  ChevronRight,
  FileText,
  FileCheck,
  MessageSquare,
  HelpCircle,
  Send,
  History,
  BarChart3,
  CheckCircle,
  XCircle,
  AlertCircle
} from 'lucide-react';
import api from '../services/api';

const SUPPORTED_ROLES = [
  'Full Stack Developer',
  'Frontend Developer',
  'Backend Developer',
  'Data Analyst',
  'Data Scientist',
  'Machine Learning Engineer',
  'AI Engineer'
];

export default function CareerPage() {
  const [activeTab, setActiveTab] = useState('profile'); // 'profile' | 'resume' | 'interview'

  // Career Profile State
  const [profile, setProfile] = useState(null);
  const [skillGapAnalysis, setSkillGapAnalysis] = useState(null);
  const [loading, setLoading] = useState(true);

  // Form State for Profile
  const [careerGoal, setCareerGoal] = useState('Become a Full Stack Software Engineer');
  const [targetRole, setTargetRole] = useState('Full Stack Developer');
  const [domain, setDomain] = useState('Software Engineering');
  const [isEditingProfile, setIsEditingProfile] = useState(false);

  // Form State for adding skills
  const [newSkillName, setNewSkillName] = useState('');
  const [newProficiency, setNewProficiency] = useState('INTERMEDIATE');

  // AI Advisor & Roadmap State
  const [advisorResult, setAdvisorResult] = useState(null);
  const [advisorLoading, setAdvisorLoading] = useState(false);
  const [roadmapResult, setRoadmapResult] = useState(null);
  const [roadmapLoading, setRoadmapLoading] = useState(false);

  // -------------------------------------------------------------
  // RESUME INTELLIGENCE STATE
  // -------------------------------------------------------------
  const [resumes, setResumes] = useState([]);
  const [selectedResumeId, setSelectedResumeId] = useState(null);
  const [resumeTitle, setResumeTitle] = useState('');
  const [resumeText, setResumeText] = useState('');
  const [resumeRole, setResumeRole] = useState('Full Stack Developer');
  const [isCreatingResume, setIsCreatingResume] = useState(false);
  const [editingResumeId, setEditingResumeId] = useState(null);
  const [resumeAnalyzingId, setResumeAnalyzingId] = useState(null);
  const [activeAnalysis, setActiveAnalysis] = useState(null);

  // -------------------------------------------------------------
  // INTERVIEW INTELLIGENCE STATE
  // -------------------------------------------------------------
  const [interviewRole, setInterviewRole] = useState('Full Stack Developer');
  const [interviewCategory, setInterviewCategory] = useState('TECHNICAL');
  const [interviewDifficulty, setInterviewDifficulty] = useState('MEDIUM');
  const [interviewSkill, setInterviewSkill] = useState('');
  const [isGeneratingQuestion, setIsGeneratingQuestion] = useState(false);
  const [currentQuestion, setCurrentQuestion] = useState(null);

  // Answer & Evaluation State
  const [studentAnswer, setStudentAnswer] = useState('');
  const [isSubmittingAnswer, setIsSubmittingAnswer] = useState(false);
  const [evaluationResult, setEvaluationResult] = useState(null);

  // History & Weak Areas State
  const [interviewHistoryData, setInterviewHistoryData] = useState(null);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // Fetch Career Profile
  const fetchProfile = async () => {
    try {
      const res = await api.get('/career/profile');
      if (res.data.success && res.data.data) {
        setProfile(res.data.data.profile);
        setSkillGapAnalysis(res.data.data.skillGapAnalysis);
        if (res.data.data.profile) {
          setCareerGoal(res.data.data.profile.career_goal || '');
          setTargetRole(res.data.data.profile.target_role || 'Full Stack Developer');
          setDomain(res.data.data.profile.domain || 'Software Engineering');
          setResumeRole(res.data.data.profile.target_role || 'Full Stack Developer');
          setInterviewRole(res.data.data.profile.target_role || 'Full Stack Developer');
        }
      }
    } catch (err) {
      console.warn('Career profile fetch fallback');
    } finally {
      setLoading(false);
    }
  };

  // Fetch Resumes
  const fetchResumes = async () => {
    try {
      const res = await api.get('/career/resumes');
      if (res.data.success && res.data.data) {
        setResumes(res.data.data);
        if (res.data.data.length > 0 && !selectedResumeId) {
          setSelectedResumeId(res.data.data[0].id);
          if (res.data.data[0].analysis) {
            setActiveAnalysis(res.data.data[0].analysis);
          }
        }
      }
    } catch (err) {
      console.error('Error fetching resumes', err);
    }
  };

  // Fetch Interview History
  const fetchInterviewHistory = async () => {
    setLoadingHistory(true);
    try {
      const res = await api.get('/career/interview/history');
      if (res.data.success && res.data.data) {
        setInterviewHistoryData(res.data.data);
      }
    } catch (err) {
      console.error('Error fetching interview history', err);
    } finally {
      setLoadingHistory(false);
    }
  };

  useEffect(() => {
    fetchProfile();
    fetchResumes();
    fetchInterviewHistory();
  }, []);

  // Profile Save
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/career/profile', {
        career_goal: careerGoal,
        target_role: targetRole,
        domain
      });
      if (res.data.success) {
        setProfile(res.data.data.profile);
        setSkillGapAnalysis(res.data.data.skillGapAnalysis);
        setIsEditingProfile(false);
      }
    } catch (err) {
      console.error(err);
    }
  };

  // Add Skill
  const handleAddSkill = async (e) => {
    e.preventDefault();
    if (!newSkillName.trim()) return;

    try {
      const res = await api.post('/career/skills', {
        skill: newSkillName.trim(),
        proficiency: newProficiency
      });
      if (res.data.success) {
        setProfile(res.data.data.profile);
        setSkillGapAnalysis(res.data.data.skillGapAnalysis);
        setNewSkillName('');
      }
    } catch (err) {
      console.error(err);
    }
  };

  // Delete Skill
  const handleDeleteSkill = async (skillName) => {
    try {
      const res = await api.delete(`/career/skills/${encodeURIComponent(skillName)}`);
      if (res.data.success) {
        setProfile(res.data.data.profile);
        setSkillGapAnalysis(res.data.data.skillGapAnalysis);
      }
    } catch (err) {
      console.error(err);
    }
  };

  // Run Advisor
  const handleRunAdvisor = async () => {
    setAdvisorLoading(true);
    setAdvisorResult(null);
    try {
      const res = await api.post('/career/advisor');
      if (res.data.success) {
        setAdvisorResult(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setAdvisorLoading(false);
    }
  };

  // Run Roadmap
  const handleRunRoadmap = async () => {
    setRoadmapLoading(true);
    setRoadmapResult(null);
    try {
      const res = await api.post('/career/roadmap');
      if (res.data.success) {
        setRoadmapResult(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setRoadmapLoading(false);
    }
  };

  // -------------------------------------------------------------
  // RESUME HANDLERS
  // -------------------------------------------------------------
  const handleSaveResume = async (e) => {
    e.preventDefault();
    if (!resumeText.trim()) return;

    try {
      if (editingResumeId) {
        const res = await api.put(`/career/resumes/${editingResumeId}`, {
          title: resumeTitle,
          resume_text: resumeText,
          target_role: resumeRole
        });
        if (res.data.success) {
          setResumes(prev => prev.map(r => r.id === editingResumeId ? res.data.data : r));
          setEditingResumeId(null);
          setIsCreatingResume(false);
        }
      } else {
        const res = await api.post('/career/resumes', {
          title: resumeTitle || `Resume Version (${new Date().toLocaleDateString()})`,
          resume_text: resumeText,
          target_role: resumeRole
        });
        if (res.data.success) {
          setResumes(prev => [res.data.data, ...prev]);
          setSelectedResumeId(res.data.data.id);
          setIsCreatingResume(false);
          setResumeText('');
          setResumeTitle('');
        }
      }
    } catch (err) {
      console.error('Error saving resume', err);
      alert(err.response?.data?.error || 'Failed to save resume');
    }
  };

  const handleDeleteResume = async (id) => {
    if (!window.confirm('Are you sure you want to delete this resume version?')) return;
    try {
      const res = await api.delete(`/career/resumes/${id}`);
      if (res.data.success) {
        setResumes(prev => prev.filter(r => r.id !== id));
        if (selectedResumeId === id) {
          setSelectedResumeId(null);
          setActiveAnalysis(null);
        }
      }
    } catch (err) {
      console.error('Error deleting resume', err);
    }
  };

  const handleAnalyzeResume = async (id) => {
    setResumeAnalyzingId(id);
    setActiveAnalysis(null);
    try {
      const res = await api.post(`/career/resumes/${id}/analyze`);
      if (res.data.success) {
        setActiveAnalysis(res.data.data);
        setSelectedResumeId(id);
        // Update resume in state with analysis
        setResumes(prev => prev.map(r => r.id === id ? { ...r, analysis: res.data.data } : r));
      }
    } catch (err) {
      console.error('Error analyzing resume', err);
      alert(err.response?.data?.error || 'Failed to analyze resume');
    } finally {
      setResumeAnalyzingId(null);
    }
  };

  const handleEditResumeClick = (resume) => {
    setEditingResumeId(resume.id);
    setResumeTitle(resume.title);
    setResumeText(resume.resume_text);
    setResumeRole(resume.target_role);
    setIsCreatingResume(true);
  };

  // -------------------------------------------------------------
  // INTERVIEW HANDLERS
  // -------------------------------------------------------------
  const handleGenerateQuestion = async () => {
    setIsGeneratingQuestion(true);
    setEvaluationResult(null);
    setStudentAnswer('');
    try {
      const res = await api.post('/career/interview/generate', {
        targetRole: interviewRole,
        category: interviewCategory,
        difficulty: interviewDifficulty,
        skill: interviewSkill || undefined
      });
      if (res.data.success) {
        setCurrentQuestion(res.data.data);
      }
    } catch (err) {
      console.error('Error generating question', err);
      alert(err.response?.data?.error || 'Failed to generate interview question');
    } finally {
      setIsGeneratingQuestion(false);
    }
  };

  const handleSubmitAnswer = async (e) => {
    e.preventDefault();
    if (!currentQuestion || !studentAnswer.trim()) return;

    setIsSubmittingAnswer(true);
    try {
      const res = await api.post(`/career/interview/${currentQuestion.id}/answer`, {
        answer: studentAnswer.trim()
      });
      if (res.data.success) {
        setEvaluationResult(res.data.data);
        fetchInterviewHistory(); // Refresh history & weak areas
      }
    } catch (err) {
      console.error('Error submitting answer', err);
      alert(err.response?.data?.error || 'Failed to submit answer');
    } finally {
      setIsSubmittingAnswer(false);
    }
  };

  if (loading) return <div className="glass-card" style={{ padding: '60px', textAlign: 'center' }}>⏳ Loading Career Intelligence Profile...</div>;

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header bar with Navigation Tabs */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Briefcase style={{ width: '24px', height: '24px', color: '#f59e0b' }} /> Career & Interview Intelligence
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Authoritative skill gap analytics, resume coverage metrics, and structured AI interview practice.
          </p>
        </div>

        {/* Tab Switcher */}
        <div style={{ display: 'flex', background: 'rgba(0,0,0,0.25)', padding: '4px', borderRadius: '10px', gap: '4px' }}>
          <button 
            className={`btn ${activeTab === 'profile' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setActiveTab('profile')}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Target style={{ width: '15px', height: '15px' }} /> Profile & Skill Gaps
          </button>
          <button 
            className={`btn ${activeTab === 'resume' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setActiveTab('resume')}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <FileText style={{ width: '15px', height: '15px' }} /> Resume Intelligence
          </button>
          <button 
            className={`btn ${activeTab === 'interview' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setActiveTab('interview')}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <MessageSquare style={{ width: '15px', height: '15px' }} /> Interview Practice
          </button>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* TAB 1: CAREER PROFILE & SKILL GAPS                                         */}
      {/* ========================================================================= */}
      {activeTab === 'profile' && (
        <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <button className="btn btn-secondary" onClick={handleRunAdvisor} disabled={advisorLoading}>
              <Brain style={{ width: '16px', height: '16px', color: '#f59e0b' }} /> {advisorLoading ? 'Analyzing...' : 'AI Career Advisor'}
            </button>
            <button className="btn btn-primary" onClick={handleRunRoadmap} disabled={roadmapLoading}>
              <Map style={{ width: '16px', height: '16px' }} /> {roadmapLoading ? 'Building...' : 'Generate Roadmap'}
            </button>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '20px' }}>
            
            {/* Career Profile Card */}
            <div className="glass-card" style={{ padding: '24px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Target style={{ width: '18px', height: '18px', color: '#6ee7b7' }} /> Career Profile Target
                </h3>
                <button className="btn btn-secondary btn-sm" onClick={() => setIsEditingProfile(!isEditingProfile)}>
                  <Edit3 style={{ width: '14px', height: '14px' }} /> {isEditingProfile ? 'Cancel' : 'Edit Profile'}
                </button>
              </div>

              {isEditingProfile ? (
                <form onSubmit={handleSaveProfile} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <div>
                    <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Career Goal</label>
                    <input type="text" className="input-field" required value={careerGoal} onChange={e=>setCareerGoal(e.target.value)} />
                  </div>

                  <div>
                    <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Target Job Role</label>
                    <select className="input-field" value={targetRole} onChange={e=>setTargetRole(e.target.value)}>
                      {SUPPORTED_ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                    </select>
                  </div>

                  <div>
                    <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Domain / Industry Focus</label>
                    <input type="text" className="input-field" value={domain} onChange={e=>setDomain(e.target.value)} />
                  </div>

                  <button type="submit" className="btn btn-primary" style={{ marginTop: '8px' }}>Save Profile</button>
                </form>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  <div>
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Target Role</span>
                    <div style={{ fontSize: '1.15rem', fontWeight: 800, color: '#38bdf8' }}>{profile?.target_role || targetRole}</div>
                  </div>

                  <div>
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Career Goal</span>
                    <div style={{ fontSize: '0.95rem', color: '#e2e8f0', fontWeight: 500 }}>{profile?.career_goal || 'None specified'}</div>
                  </div>

                  <div>
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Domain</span>
                    <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>{profile?.domain || 'Software Engineering'}</div>
                  </div>
                </div>
              )}

              {/* Skills Management Section */}
              <div style={{ marginTop: '24px', paddingTop: '16px', borderTop: '1px solid var(--border-color)' }}>
                <h4 style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Award style={{ width: '16px', height: '16px', color: '#f59e0b' }} /> Verified Profile Skills ({profile?.skills?.length || 0})
                </h4>

                <form onSubmit={handleAddSkill} style={{ display: 'flex', gap: '8px', marginBottom: '14px' }}>
                  <input 
                    type="text" 
                    placeholder="e.g. Docker, React, SQL..." 
                    className="input-field" 
                    style={{ flex: 1 }}
                    value={newSkillName}
                    onChange={e=>setNewSkillName(e.target.value)}
                  />
                  <select 
                    className="input-field" 
                    style={{ width: '130px' }}
                    value={newProficiency}
                    onChange={e=>setNewProficiency(e.target.value)}
                  >
                    <option value="BEGINNER">BEGINNER</option>
                    <option value="INTERMEDIATE">INTERMEDIATE</option>
                    <option value="ADVANCED">ADVANCED</option>
                  </select>
                  <button type="submit" className="btn btn-secondary" style={{ padding: '0 12px' }}>
                    <Plus style={{ width: '16px', height: '16px' }} />
                  </button>
                </form>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', maxHeight: '180px', overflowY: 'auto' }}>
                  {profile?.skills?.map((s, idx) => (
                    <div 
                      key={idx}
                      style={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        gap: '6px', 
                        padding: '4px 10px', 
                        borderRadius: '20px', 
                        background: 'rgba(255, 255, 255, 0.05)',
                        border: '1px solid var(--border-color)',
                        fontSize: '0.82rem'
                      }}
                    >
                      <span><strong>{s.skill}</strong> ({s.proficiency})</span>
                      <button 
                        type="button" 
                        onClick={() => handleDeleteSkill(s.skill)}
                        style={{ background: 'transparent', border: 'none', color: '#f87171', cursor: 'pointer', padding: '2px' }}
                      >
                        <Trash2 style={{ width: '12px', height: '12px' }} />
                      </button>
                    </div>
                  ))}
                  {(!profile?.skills || profile.skills.length === 0) && (
                    <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>No skills added yet. Add your current skills to calculate role readiness.</p>
                  )}
                </div>
              </div>
            </div>

            {/* Deterministic Skill Gap Analysis Card */}
            <div className="glass-card" style={{ padding: '24px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
                <Layers style={{ width: '18px', height: '18px', color: '#818cf8' }} /> Authoritative Skill-Gap Intelligence
              </h3>

              {skillGapAnalysis && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                  {/* Coverage Percentage Gauge */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(0,0,0,0.2)', padding: '16px', borderRadius: '12px' }}>
                    <div>
                      <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Target Role Competency Coverage</div>
                      <div style={{ fontSize: '2rem', fontWeight: 900, color: skillGapAnalysis.coveragePercentage >= 70 ? '#34d399' : skillGapAnalysis.coveragePercentage >= 40 ? '#fbbf24' : '#f87171' }}>
                        {skillGapAnalysis.coveragePercentage}%
                      </div>
                    </div>
                    <div style={{ textAlign: 'right', fontSize: '0.85rem' }}>
                      <div style={{ color: '#6ee7b7' }}>✓ Strong: <strong>{skillGapAnalysis.strongCount}</strong></div>
                      <div style={{ color: '#fde047' }}>⚡ Partial: <strong>{skillGapAnalysis.partialCount}</strong></div>
                      <div style={{ color: '#fca5a5' }}>✗ Gaps: <strong>{skillGapAnalysis.gapCount}</strong></div>
                    </div>
                  </div>

                  {/* Breakdown Matrix */}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#c7d2fe' }}>Required Role Competencies:</div>
                    {skillGapAnalysis.skillAnalysis?.map((item, idx) => (
                      <div 
                        key={idx}
                        style={{
                          padding: '10px 14px',
                          borderRadius: '8px',
                          background: item.classification === 'STRONG' ? 'rgba(16, 185, 129, 0.06)' : item.classification === 'PARTIAL' ? 'rgba(245, 158, 11, 0.06)' : 'rgba(244, 63, 94, 0.06)',
                          border: item.classification === 'STRONG' ? '1px solid rgba(16, 185, 129, 0.2)' : item.classification === 'PARTIAL' ? '1px solid rgba(245, 158, 11, 0.2)' : '1px solid rgba(244, 63, 94, 0.2)',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center'
                        }}
                      >
                        <div>
                          <strong style={{ fontSize: '0.92rem' }}>{item.skill}</strong>
                          <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginLeft: '8px' }}>
                            (You: {item.studentProficiency})
                          </span>
                        </div>

                        <span className={`badge ${item.classification === 'STRONG' ? 'badge-emerald' : item.classification === 'PARTIAL' ? 'badge-amber' : 'badge-rose'}`}>
                          {item.classification}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

          </div>

          {/* AI Career Advisor Results */}
          {advisorResult && (
            <div className="glass-card animate-fade-in" style={{ padding: '24px', background: 'linear-gradient(135deg, rgba(30, 27, 75, 0.9), rgba(15, 23, 42, 0.95))', border: '1px solid rgba(245, 158, 11, 0.3)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#fde047', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Brain style={{ width: '18px', height: '18px' }} /> Gemini AI Career Advisory Strategy
                </h3>
                {advisorResult.isFallback && <span className="badge badge-amber">Deterministic Advisory</span>}
              </div>
              <p style={{ fontSize: '0.92rem', color: '#e2e8f0', lineHeight: '1.6', whiteSpace: 'pre-line' }}>
                {advisorResult.recommendation}
              </p>
            </div>
          )}

          {/* Learning Roadmap Results */}
          {roadmapResult && (
            <div className="glass-card animate-fade-in" style={{ padding: '24px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#67e8f9', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
                <Map style={{ width: '20px', height: '20px' }} /> Personalized Skill-Gap Learning Roadmap
              </h3>
              <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', marginBottom: '16px' }}>{roadmapResult.summary}</p>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
                {roadmapResult.phases?.map((p, idx) => (
                  <div key={idx} style={{ padding: '16px', borderRadius: '10px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                      <span className="badge badge-cyan">{p.phase}</span>
                      {p.effort && <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>⏱️ {p.effort}</span>}
                    </div>
                    <div style={{ fontWeight: 700, fontSize: '0.95rem', margin: '6px 0', color: '#f8fafc' }}>
                      Target Skills: {p.skills?.join(', ')}
                    </div>
                    <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>{p.focus}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: RESUME INTELLIGENCE                                                */}
      {/* ========================================================================= */}
      {activeTab === 'resume' && (
        <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          {/* Header Action Bar */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <FileCheck style={{ width: '20px', height: '20px', color: '#38bdf8' }} /> Student Resume Repository ({resumes.length})
            </h3>
            <button 
              className="btn btn-primary"
              onClick={() => {
                setIsCreatingResume(!isCreatingResume);
                setEditingResumeId(null);
                setResumeTitle('');
                setResumeText('');
              }}
            >
              <Plus style={{ width: '16px', height: '16px' }} /> {isCreatingResume ? 'Close Editor' : 'Add Resume Version'}
            </button>
          </div>

          {/* Resume Form (Create / Edit) */}
          {isCreatingResume && (
            <div className="glass-card animate-fade-in" style={{ padding: '24px' }}>
              <h4 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '14px' }}>
                {editingResumeId ? '✏️ Edit Resume Version' : '📄 Enter / Paste Resume Content'}
              </h4>
              <form onSubmit={handleSaveResume} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '12px' }}>
                  <div>
                    <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Resume Title</label>
                    <input 
                      type="text" 
                      className="input-field" 
                      placeholder="e.g. Full Stack Engineering Resume 2026"
                      value={resumeTitle} 
                      onChange={e=>setResumeTitle(e.target.value)} 
                    />
                  </div>
                  <div>
                    <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Target Job Role</label>
                    <select className="input-field" value={resumeRole} onChange={e=>setResumeRole(e.target.value)}>
                      {SUPPORTED_ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                    </select>
                  </div>
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>
                    Resume Content (Paste Text, Experience, Projects & Skills)
                  </label>
                  <textarea 
                    className="input-field" 
                    rows={10} 
                    required
                    placeholder="Paste plain text of your resume here... E.g., Summary, Experience, Technical Skills (JavaScript, React, Node.js, Docker), Projects, Education."
                    style={{ fontFamily: 'monospace', fontSize: '0.85rem', lineHeight: '1.5' }}
                    value={resumeText}
                    onChange={e=>setResumeText(e.target.value)}
                  />
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'right', marginTop: '4px' }}>
                    Character count: {resumeText.length} / 50,000 max
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                  <button type="button" className="btn btn-secondary" onClick={() => setIsCreatingResume(false)}>Cancel</button>
                  <button type="submit" className="btn btn-primary">{editingResumeId ? 'Update Resume' : 'Save Resume'}</button>
                </div>
              </form>
            </div>
          )}

          {/* Stored Resumes List */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
            {resumes.map(r => (
              <div 
                key={r.id} 
                className="glass-card" 
                style={{ 
                  padding: '18px', 
                  border: selectedResumeId === r.id ? '2px solid #38bdf8' : '1px solid var(--border-color)',
                  display: 'flex', 
                  flexDirection: 'column', 
                  justifyContent: 'space-between',
                  gap: '12px'
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
                    <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc' }}>{r.title}</h4>
                    <span className="badge badge-cyan">{r.target_role}</span>
                  </div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: '10px' }}>
                    Updated: {new Date(r.updated_at || r.created_at).toLocaleDateString()}
                  </div>
                  <p style={{ fontSize: '0.82rem', color: '#94a3b8', maxHeight: '60px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {r.resume_text.slice(0, 160)}...
                  </p>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '10px', borderTop: '1px solid var(--border-color)' }}>
                  <div style={{ display: 'flex', gap: '6px' }}>
                    <button 
                      className="btn btn-secondary btn-sm" 
                      onClick={() => handleEditResumeClick(r)}
                      title="Edit resume"
                    >
                      <Edit3 style={{ width: '13px', height: '13px' }} />
                    </button>
                    <button 
                      className="btn btn-secondary btn-sm" 
                      onClick={() => handleDeleteResume(r.id)}
                      title="Delete resume"
                      style={{ color: '#f87171' }}
                    >
                      <Trash2 style={{ width: '13px', height: '13px' }} />
                    </button>
                  </div>

                  <button 
                    className="btn btn-primary btn-sm"
                    disabled={resumeAnalyzingId === r.id}
                    onClick={() => handleAnalyzeResume(r.id)}
                  >
                    <Brain style={{ width: '14px', height: '14px' }} /> 
                    {resumeAnalyzingId === r.id ? 'Analyzing...' : (r.analysis ? 'Re-Analyze' : 'Analyze')}
                  </button>
                </div>
              </div>
            ))}

            {resumes.length === 0 && (
              <div className="glass-card" style={{ padding: '36px', textAlign: 'center', gridColumn: '1 / -1' }}>
                <p style={{ color: 'var(--text-muted)', marginBottom: '12px' }}>No resumes stored. Paste your resume to analyze role coverage.</p>
                <button className="btn btn-primary btn-sm" onClick={() => setIsCreatingResume(true)}>
                  <Plus style={{ width: '15px', height: '15px' }} /> Create First Resume
                </button>
              </div>
            )}
          </div>

          {/* Active Resume Analysis Results Display */}
          {activeAnalysis && (
            <div className="glass-card animate-fade-in" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
              
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#38bdf8', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Sparkles style={{ width: '20px', height: '20px' }} /> Resume Intelligence & Skill Gap Report
                </h3>
                <span className="badge badge-emerald">Target: {activeAnalysis.targetRole}</span>
              </div>

              {/* Deterministic Metric Summary */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '14px' }}>
                <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)' }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Role Skill Coverage</div>
                  <div style={{ fontSize: '2rem', fontWeight: 900, color: activeAnalysis.coverage?.coveragePercentage >= 70 ? '#34d399' : activeAnalysis.coverage?.coveragePercentage >= 40 ? '#fbbf24' : '#f87171' }}>
                    {activeAnalysis.coverage?.coveragePercentage}%
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                    Formula: {activeAnalysis.coverage?.coverageFormula}
                  </div>
                </div>

                <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)' }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Matched Role Skills</div>
                  <div style={{ fontSize: '2rem', fontWeight: 900, color: '#6ee7b7' }}>
                    {activeAnalysis.coverage?.matchedCount} <span style={{ fontSize: '1rem', color: 'var(--text-muted)' }}>/ {activeAnalysis.coverage?.totalRequired}</span>
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                    Competencies verified in text
                  </div>
                </div>

                <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)' }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Missing Role Skills</div>
                  <div style={{ fontSize: '2rem', fontWeight: 900, color: '#fca5a5' }}>
                    {activeAnalysis.coverage?.missingCount}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                    Required skills with no evidence
                  </div>
                </div>
              </div>

              {/* Skills Grid: Matched & Missing */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
                <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(16, 185, 129, 0.05)', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
                  <div style={{ fontSize: '0.88rem', fontWeight: 700, color: '#6ee7b7', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <CheckCircle style={{ width: '16px', height: '16px' }} /> Detected Skills in Resume:
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                    {activeAnalysis.coverage?.matchedSkills?.map((s, idx) => (
                      <span key={idx} className="badge badge-emerald">
                        {s} <span style={{ fontSize: '0.68rem', opacity: 0.8, marginLeft: '4px' }}>DETECTED_IN_RESUME</span>
                      </span>
                    ))}
                    {(!activeAnalysis.coverage?.matchedSkills || activeAnalysis.coverage.matchedSkills.length === 0) && (
                      <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>No matching required skills detected yet.</span>
                    )}
                  </div>
                </div>

                <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(244, 63, 94, 0.05)', border: '1px solid rgba(244, 63, 94, 0.2)' }}>
                  <div style={{ fontSize: '0.88rem', fontWeight: 700, color: '#fca5a5', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <XCircle style={{ width: '16px', height: '16px' }} /> Missing Role Skills (Resume Gaps):
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                    {activeAnalysis.coverage?.missingSkills?.map((s, idx) => (
                      <span key={idx} className="badge badge-rose">
                        {s}
                      </span>
                    ))}
                    {(!activeAnalysis.coverage?.missingSkills || activeAnalysis.coverage.missingSkills.length === 0) && (
                      <span style={{ fontSize: '0.82rem', color: '#6ee7b7' }}>🎉 100% role skill representation in resume!</span>
                    )}
                  </div>
                </div>
              </div>

              {/* AI Qualitative Feedback */}
              <div style={{ padding: '18px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--border-color)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#fde047', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <Brain style={{ width: '16px', height: '16px' }} /> AI Improvement Advisor & Actionable Suggestions
                  </h4>
                  <span className="badge badge-amber">AI Feedback</span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px', marginBottom: '14px' }}>
                  <div>
                    <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#6ee7b7', marginBottom: '6px' }}>Resume Strengths:</div>
                    <ul style={{ paddingLeft: '18px', fontSize: '0.85rem', color: '#cbd5e1', lineHeight: '1.6' }}>
                      {activeAnalysis.aiSuggestions?.strengths?.map((st, i) => (
                        <li key={i}>{st}</li>
                      ))}
                    </ul>
                  </div>

                  <div>
                    <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#fca5a5', marginBottom: '6px' }}>Areas Needing Improvement:</div>
                    <ul style={{ paddingLeft: '18px', fontSize: '0.85rem', color: '#cbd5e1', lineHeight: '1.6' }}>
                      {activeAnalysis.aiSuggestions?.improvements?.map((imp, i) => (
                        <li key={i}>{imp}</li>
                      ))}
                    </ul>
                  </div>
                </div>

                <div>
                  <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#67e8f9', marginBottom: '6px' }}>Actionable Recommendations:</div>
                  <ul style={{ paddingLeft: '18px', fontSize: '0.85rem', color: '#e2e8f0', lineHeight: '1.6' }}>
                    {activeAnalysis.aiSuggestions?.actionableSuggestions?.map((sug, i) => (
                      <li key={i}>{sug}</li>
                    ))}
                  </ul>
                </div>
              </div>

            </div>
          )}

        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 3: INTERVIEW PRACTICE & WEAK AREAS                                    */}
      {/* ========================================================================= */}
      {activeTab === 'interview' && (
        <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          {/* Question Generator Bar */}
          <div className="glass-card" style={{ padding: '24px' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <HelpCircle style={{ width: '20px', height: '20px', color: '#f59e0b' }} /> Targeted Interview Question Generator
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', alignItems: 'flex-end' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Target Role</label>
                <select className="input-field" value={interviewRole} onChange={e=>setInterviewRole(e.target.value)}>
                  {SUPPORTED_ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Interview Category</label>
                <select className="input-field" value={interviewCategory} onChange={e=>setInterviewCategory(e.target.value)}>
                  <option value="TECHNICAL">TECHNICAL</option>
                  <option value="BEHAVIORAL">BEHAVIORAL</option>
                  <option value="PROJECT">PROJECT</option>
                  <option value="ROLE_SPECIFIC">ROLE_SPECIFIC</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Difficulty</label>
                <select className="input-field" value={interviewDifficulty} onChange={e=>setInterviewDifficulty(e.target.value)}>
                  <option value="EASY">EASY</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HARD">HARD</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Focus Skill (Optional)</label>
                <input 
                  type="text" 
                  className="input-field" 
                  placeholder="e.g. Docker, SQL, React" 
                  value={interviewSkill} 
                  onChange={e=>setInterviewSkill(e.target.value)} 
                />
              </div>

              <div>
                <button 
                  className="btn btn-primary" 
                  style={{ width: '100%', height: '42px' }}
                  disabled={isGeneratingQuestion}
                  onClick={handleGenerateQuestion}
                >
                  <Brain style={{ width: '16px', height: '16px' }} /> 
                  {isGeneratingQuestion ? 'Generating...' : 'Generate Question'}
                </button>
              </div>
            </div>
          </div>

          {/* Active Question & Practice Card */}
          {currentQuestion && (
            <div className="glass-card animate-fade-in" style={{ padding: '24px', border: '1px solid rgba(99, 102, 241, 0.4)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', flexWrap: 'wrap', gap: '8px' }}>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <span className="badge badge-indigo">{currentQuestion.category}</span>
                  <span className={`badge ${currentQuestion.difficulty === 'HARD' ? 'badge-rose' : currentQuestion.difficulty === 'MEDIUM' ? 'badge-amber' : 'badge-emerald'}`}>
                    {currentQuestion.difficulty}
                  </span>
                  <span className="badge badge-cyan">{currentQuestion.target_skill}</span>
                </div>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Role: {currentQuestion.target_role}</span>
              </div>

              <h4 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#f8fafc', margin: '14px 0', lineHeight: '1.5' }}>
                "{currentQuestion.question}"
              </h4>

              {currentQuestion.preparation_tips && (
                <div style={{ padding: '10px 14px', borderRadius: '8px', background: 'rgba(99, 102, 241, 0.08)', border: '1px solid rgba(99, 102, 241, 0.2)', fontSize: '0.85rem', color: '#c7d2fe', marginBottom: '16px' }}>
                  💡 <strong>Preparation Tip:</strong> {currentQuestion.preparation_tips}
                </div>
              )}

              {/* Answer Input */}
              <form onSubmit={handleSubmitAnswer} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <label style={{ fontSize: '0.85rem', fontWeight: 600, color: '#e2e8f0' }}>Your Response / Explanation:</label>
                <textarea 
                  className="input-field" 
                  rows={6}
                  required
                  placeholder="Type your structured answer here. Include concrete mechanisms, architectural trade-offs, or STAR situation details..."
                  style={{ fontSize: '0.9rem', lineHeight: '1.5' }}
                  value={studentAnswer}
                  onChange={e=>setStudentAnswer(e.target.value)}
                />

                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <button 
                    type="submit" 
                    className="btn btn-primary"
                    disabled={isSubmittingAnswer || studentAnswer.trim().length < 5}
                    style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
                  >
                    <Send style={{ width: '15px', height: '15px' }} />
                    {isSubmittingAnswer ? 'Evaluating Answer...' : 'Submit Answer for AI Feedback'}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* AI Evaluation & Deterministic Score Card */}
          {evaluationResult && (
            <div className="glass-card animate-fade-in" style={{ padding: '24px', background: 'linear-gradient(135deg, rgba(15, 23, 42, 0.95), rgba(30, 27, 75, 0.9))', border: '1px solid rgba(52, 211, 153, 0.4)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#34d399', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Award style={{ width: '20px', height: '20px' }} /> Interview Performance Evaluation
                </h3>
                <span className="badge badge-emerald">Deterministic Score</span>
              </div>

              {/* Dimension Scores Matrix */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '12px', marginBottom: '20px' }}>
                <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(0,0,0,0.3)', textAlign: 'center' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Relevance</div>
                  <div style={{ fontSize: '1.3rem', fontWeight: 800, color: '#38bdf8' }}>{evaluationResult.scores?.relevance} / 5</div>
                </div>
                <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(0,0,0,0.3)', textAlign: 'center' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Clarity</div>
                  <div style={{ fontSize: '1.3rem', fontWeight: 800, color: '#a78bfa' }}>{evaluationResult.scores?.clarity} / 5</div>
                </div>
                <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(0,0,0,0.3)', textAlign: 'center' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Completeness</div>
                  <div style={{ fontSize: '1.3rem', fontWeight: 800, color: '#f472b6' }}>{evaluationResult.scores?.completeness} / 5</div>
                </div>
                <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(0,0,0,0.3)', textAlign: 'center' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Technical</div>
                  <div style={{ fontSize: '1.3rem', fontWeight: 800, color: '#34d399' }}>{evaluationResult.scores?.technical_understanding} / 5</div>
                </div>
                <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', textAlign: 'center' }}>
                  <div style={{ fontSize: '0.75rem', color: '#6ee7b7' }}>Total Percentage</div>
                  <div style={{ fontSize: '1.4rem', fontWeight: 900, color: '#34d399' }}>{evaluationResult.scores?.percentage}%</div>
                </div>
              </div>

              {/* Feedback Content */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ fontSize: '0.92rem', color: '#e2e8f0', lineHeight: '1.6' }}>
                  <strong>Evaluator Summary:</strong> {evaluationResult.feedback?.overall_feedback}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
                  <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(16, 185, 129, 0.05)', border: '1px solid rgba(16, 185, 129, 0.15)' }}>
                    <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#6ee7b7', marginBottom: '6px' }}>What Went Well:</div>
                    <ul style={{ paddingLeft: '18px', fontSize: '0.82rem', color: '#cbd5e1', lineHeight: '1.5' }}>
                      {evaluationResult.feedback?.strengths?.map((s, idx) => <li key={idx}>{s}</li>)}
                    </ul>
                  </div>

                  <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(244, 63, 94, 0.05)', border: '1px solid rgba(244, 63, 94, 0.15)' }}>
                    <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#fca5a5', marginBottom: '6px' }}>Areas to Polish:</div>
                    <ul style={{ paddingLeft: '18px', fontSize: '0.82rem', color: '#cbd5e1', lineHeight: '1.5' }}>
                      {evaluationResult.feedback?.improvements?.map((s, idx) => <li key={idx}>{s}</li>)}
                    </ul>
                  </div>
                </div>

                {evaluationResult.feedback?.follow_up_tip && (
                  <div style={{ fontSize: '0.85rem', color: '#fde047', background: 'rgba(245, 158, 11, 0.08)', padding: '10px 14px', borderRadius: '8px', border: '1px solid rgba(245, 158, 11, 0.2)' }}>
                    🎯 <strong>Next Actionable Step:</strong> {evaluationResult.feedback?.follow_up_tip}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Interview History & Weak Areas Diagnostics */}
          <div className="glass-card" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <History style={{ width: '18px', height: '18px', color: '#38bdf8' }} /> Practice History & Weak Area Diagnostics
              </h3>
              {interviewHistoryData && (
                <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Total Practices: <strong>{interviewHistoryData.total_attempts}</strong> | Avg: <strong>{interviewHistoryData.average_score}%</strong>
                </div>
              )}
            </div>

            {/* Weak Areas Alerts */}
            {interviewHistoryData?.weak_areas && interviewHistoryData.weak_areas.length > 0 && (
              <div style={{ marginBottom: '20px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#fca5a5', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <AlertCircle style={{ width: '15px', height: '15px' }} /> Weak Practice Categories (Scores Below 70%):
                </div>
                {interviewHistoryData.weak_areas.map((w, idx) => (
                  <div 
                    key={idx}
                    style={{ 
                      padding: '10px 14px', 
                      borderRadius: '8px', 
                      background: 'rgba(244, 63, 94, 0.08)', 
                      border: '1px solid rgba(244, 63, 94, 0.25)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                  >
                    <div>
                      <strong style={{ color: '#fca5a5' }}>{w.category} Interview Questions</strong>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{w.note}</div>
                    </div>
                    <span className="badge badge-rose">{w.average_percentage}%</span>
                  </div>
                ))}
              </div>
            )}

            {/* Category Breakdown Badges */}
            {interviewHistoryData?.category_breakdown && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginBottom: '20px' }}>
                {Object.entries(interviewHistoryData.category_breakdown).map(([cat, info]) => (
                  <div 
                    key={cat}
                    style={{ 
                      padding: '8px 14px', 
                      borderRadius: '8px', 
                      background: 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid var(--border-color)',
                      fontSize: '0.82rem'
                    }}
                  >
                    <span style={{ color: 'var(--text-muted)' }}>{cat}:</span> <strong>{info.average_percentage}%</strong> ({info.attempts} sessions)
                  </div>
                ))}
              </div>
            )}

            {/* Practice History List */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {interviewHistoryData?.history?.slice(0, 5).map(h => (
                <div 
                  key={h.id}
                  style={{ 
                    padding: '12px 16px', 
                    borderRadius: '8px', 
                    background: 'rgba(255, 255, 255, 0.02)',
                    border: '1px solid var(--border-color)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: '10px'
                  }}
                >
                  <div style={{ maxWidth: '75%' }}>
                    <div style={{ display: 'flex', gap: '6px', marginBottom: '4px' }}>
                      <span className="badge badge-indigo">{h.category}</span>
                      <span className="badge badge-cyan">{h.target_skill}</span>
                    </div>
                    <div style={{ fontSize: '0.88rem', fontWeight: 600, color: '#f1f5f9' }}>{h.question}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                      Date: {new Date(h.created_at).toLocaleDateString()}
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '1.25rem', fontWeight: 900, color: h.scores?.percentage >= 70 ? '#34d399' : '#f87171' }}>
                      {h.scores?.percentage}%
                    </div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Score: {h.scores?.total} / 20</div>
                  </div>
                </div>
              ))}

              {(!interviewHistoryData?.history || interviewHistoryData.history.length === 0) && (
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', textAlign: 'center', padding: '16px' }}>
                  No interview practice sessions recorded yet. Generate a question and submit an answer above to track progress!
                </p>
              )}
            </div>

          </div>

        </div>
      )}

    </div>
  );
}
