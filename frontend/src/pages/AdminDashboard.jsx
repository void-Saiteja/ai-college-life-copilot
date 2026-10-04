import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, 
  Users, 
  FileText, 
  BookOpen, 
  Plus, 
  Trash2, 
  HelpCircle, 
  Megaphone,
  CheckCircle2,
  AlertTriangle,
  Search,
  Filter,
  Eye,
  X,
  Brain,
  Sparkles,
  BarChart3,
  Calendar,
  Layers,
  Award,
  AlertCircle,
  FileCheck,
  ChevronRight,
  TrendingDown,
  TrendingUp,
  Clock
} from 'lucide-react';
import api from '../services/api';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'students' | 'risks' | 'insights' | 'documents' | 'manage'
  
  // Dashboard Metrics & Loading
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Student Management State
  const [students, setStudents] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [semesterFilter, setSemesterFilter] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [riskFilter, setRiskFilter] = useState('');
  const [loadingStudents, setLoadingStudents] = useState(false);

  // Student Detail Modal State
  const [selectedStudentId, setSelectedStudentId] = useState(null);
  const [studentDetail, setStudentDetail] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(false);

  // Academic Risk Intelligence State
  const [riskData, setRiskData] = useState(null);
  const [loadingRisks, setLoadingRisks] = useState(false);

  // AI Executive Insights State
  const [insights, setInsights] = useState(null);
  const [loadingInsights, setLoadingInsights] = useState(false);

  // Documents Management State
  const [documents, setDocuments] = useState([]);
  const [loadingDocs, setLoadingDocs] = useState(false);

  // Modal states for creating subjects/announcements/FAQs (preserved)
  const [showSubjectModal, setShowSubjectModal] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [instructor, setInstructor] = useState('');

  const [showAncModal, setShowAncModal] = useState(false);
  const [ancTitle, setAncTitle] = useState('');
  const [ancContent, setAncContent] = useState('');

  const [legacyStats, setLegacyStats] = useState(null);

  // 1. Fetch Dashboard Overview Statistics
  const fetchDashboardStats = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.get('/admin/dashboard');
      if (res.data.success) {
        setDashboardData(res.data.data);
      }
      // Also fetch legacy stats for announcements/faqs
      const legRes = await api.get('/admin/stats');
      if (legRes.data.success) {
        setLegacyStats(legRes.data.data);
      }
    } catch (err) {
      console.error('Failed to load admin stats:', err);
      if (err.response?.status === 403) {
        setError('403 Forbidden: You do not have permission to view Admin Intelligence.');
      } else {
        setError(err.response?.data?.error || 'Failed to load administrative intelligence statistics.');
      }
    } finally {
      setLoading(false);
    }
  };

  // 2. Fetch Students List with Filters
  const fetchStudents = async () => {
    try {
      setLoadingStudents(true);
      const params = {};
      if (searchQuery) params.search = searchQuery;
      if (semesterFilter) params.semester = semesterFilter;
      if (departmentFilter) params.department = departmentFilter;
      if (riskFilter) params.attendanceRisk = riskFilter;

      const res = await api.get('/admin/students', { params });
      if (res.data.success) {
        setStudents(res.data.data);
      }
    } catch (err) {
      console.error('Failed to fetch students:', err);
    } finally {
      setLoadingStudents(false);
    }
  };

  // 3. Fetch Single Student Drill-down Detail
  const handleOpenStudentDetail = async (studentId) => {
    setSelectedStudentId(studentId);
    setLoadingDetail(true);
    setStudentDetail(null);
    try {
      const res = await api.get(`/admin/students/${studentId}`);
      if (res.data.success) {
        setStudentDetail(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load student detail:', err);
      alert('Unable to load student details.');
    } finally {
      setLoadingDetail(false);
    }
  };

  // 4. Fetch Academic Risks
  const fetchRisks = async () => {
    try {
      setLoadingRisks(true);
      const res = await api.get('/admin/risks');
      if (res.data.success) {
        setRiskData(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load risks:', err);
    } finally {
      setLoadingRisks(false);
    }
  };

  // 5. Generate AI Executive Insights
  const handleGenerateInsights = async () => {
    try {
      setLoadingInsights(true);
      const res = await api.post('/admin/insights');
      if (res.data.success) {
        setInsights(res.data.data);
      }
    } catch (err) {
      console.error('Failed to generate AI insights:', err);
    } finally {
      setLoadingInsights(false);
    }
  };

  // 6. Fetch Document / RAG Management List
  const fetchDocuments = async () => {
    try {
      setLoadingDocs(true);
      const res = await api.get('/admin/documents');
      if (res.data.success) {
        setDocuments(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load documents:', err);
    } finally {
      setLoadingDocs(false);
    }
  };

  useEffect(() => {
    fetchDashboardStats();
    fetchStudents();
    fetchRisks();
    fetchDocuments();
  }, []);

  // Filter effect for search
  useEffect(() => {
    const timer = setTimeout(() => {
      fetchStudents();
    }, 250);
    return () => clearTimeout(timer);
  }, [searchQuery, semesterFilter, departmentFilter, riskFilter]);

  // Handle Create Subject
  const handleCreateSubject = async (e) => {
    e.preventDefault();
    try {
      await api.post('/admin/subjects', { code, name, instructor });
      setCode(''); setName(''); setInstructor('');
      setShowSubjectModal(false);
      fetchDashboardStats();
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.error || 'Failed to add course subject');
    }
  };

  // Handle Create Announcement
  const handleCreateAnc = async (e) => {
    e.preventDefault();
    try {
      await api.post('/admin/announcements', { title: ancTitle, content: ancContent });
      setAncTitle(''); setAncContent('');
      setShowAncModal(false);
      fetchDashboardStats();
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.error || 'Failed to post announcement');
    }
  };

  if (loading) {
    return (
      <div className="glass-card" style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}>
        ⏳ Loading Administrative Intelligence & Security Context...
      </div>
    );
  }

  if (error) {
    return (
      <div className="glass-card" style={{ padding: '40px', maxWidth: '600px', margin: '40px auto', textAlign: 'center' }}>
        <h2 style={{ color: '#f43f5e', fontSize: '1.4rem', fontWeight: 800, marginBottom: '8px' }}>🚫 Access Restricted</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem', marginBottom: '20px' }}>{error}</p>
        <button className="btn btn-primary" onClick={() => window.history.back()}>Go Back</button>
      </div>
    );
  }

  const d = dashboardData || {};
  const activeEmbeddingModel = documents[0]?.embeddingModel || (d.documents?.embeddingProvider ? d.documents.embeddingProvider.replace('Google Gemini ', '') : d.documents?.embeddingModel) || 'gemini-embedding-2';

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Admin Header with Navigation Tabs */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ShieldCheck style={{ width: '24px', height: '24px', color: '#818cf8' }} /> Academic Administration & Intelligence Console
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Campus-wide academic metrics, student risk detection, RAG document governance, and AI executive advisory.
          </p>
        </div>

        {/* Tab Switcher */}
        <div style={{ display: 'flex', background: 'rgba(0,0,0,0.25)', padding: '4px', borderRadius: '10px', gap: '4px', flexWrap: 'wrap' }}>
          <button 
            className={`btn ${activeTab === 'overview' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setActiveTab('overview')}
          >
            Overview
          </button>
          <button 
            className={`btn ${activeTab === 'students' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setActiveTab('students')}
          >
            Students ({students.length})
          </button>
          <button 
            className={`btn ${activeTab === 'risks' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setActiveTab('risks')}
          >
            Academic Risks
          </button>
          <button 
            className={`btn ${activeTab === 'insights' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => { setActiveTab('insights'); if (!insights) handleGenerateInsights(); }}
          >
            AI Insights
          </button>
          <button 
            className={`btn ${activeTab === 'documents' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setActiveTab('documents')}
          >
            RAG Documents
          </button>
          <button 
            className={`btn ${activeTab === 'manage' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setActiveTab('manage')}
          >
            Announcements & Courses
          </button>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* TAB 1: OVERVIEW METRIC CARDS                                              */}
      {/* ========================================================================= */}
      {activeTab === 'overview' && (
        <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          {/* Top 8 Key Metrics Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
            
            {/* 1. Total Students */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Total Students</span>
                <Users style={{ width: '18px', height: '18px', color: '#38bdf8' }} />
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#f8fafc', marginTop: '6px' }}>
                {d.students?.totalStudents ?? 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                Active: <strong>{d.students?.activeStudents ?? 0}</strong> students
              </div>
            </div>

            {/* 2. Average Attendance */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Average Attendance</span>
                <BarChart3 style={{ width: '18px', height: '18px', color: '#34d399' }} />
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: (d.attendance?.averageAttendance ?? 0) >= 75 ? '#34d399' : '#fbbf24', marginTop: '6px' }}>
                {d.attendance?.averageAttendance ?? 0}%
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                Mandatory target: <strong>75%</strong>
              </div>
            </div>

            {/* 3. Students At Risk */}
            <div className="glass-card" style={{ padding: '20px', border: (d.attendance?.studentsBelowAttendanceTarget ?? 0) > 0 ? '1px solid rgba(244, 63, 94, 0.4)' : '1px solid var(--border-color)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Attendance At Risk</span>
                <AlertTriangle style={{ width: '18px', height: '18px', color: '#f87171' }} />
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#f87171', marginTop: '6px' }}>
                {d.attendance?.studentsBelowAttendanceTarget ?? 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                Breaching 75% threshold
              </div>
            </div>

            {/* 4. Pending Assignments */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Pending Assignments</span>
                <Clock style={{ width: '18px', height: '18px', color: '#fde047' }} />
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#fde047', marginTop: '6px' }}>
                {d.assignments?.pendingAssignments ?? 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                Overdue: <strong style={{ color: '#f87171' }}>{d.assignments?.overdueAssignments ?? 0}</strong>
              </div>
            </div>

            {/* 5. Upcoming Exams */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Scheduled Exams</span>
                <Calendar style={{ width: '18px', height: '18px', color: '#a78bfa' }} />
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#a78bfa', marginTop: '6px' }}>
                {d.exams?.upcomingExams ?? 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                Exams Today: <strong>{d.exams?.examsToday ?? 0}</strong>
              </div>
            </div>

            {/* 6. Average Quiz Score */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Avg Quiz Performance</span>
                <Award style={{ width: '18px', height: '18px', color: '#67e8f9' }} />
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#67e8f9', marginTop: '6px' }}>
                {d.quizzes?.averageQuizScore ?? 0}%
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                Attempts: <strong>{d.quizzes?.totalQuizAttempts ?? 0}</strong>
              </div>
            </div>

            {/* 7. Career Skill Gaps */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Students w/ Skill Gaps</span>
                <Layers style={{ width: '18px', height: '18px', color: '#fb923c' }} />
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#fb923c', marginTop: '6px' }}>
                {d.career?.studentsWithSkillGaps ?? 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                Profiles tracked: <strong>{d.career?.studentsWithCareerProfiles ?? 0}</strong>
              </div>
            </div>

            {/* 8. Documents / RAG Status */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>RAG Knowledge Base</span>
                <FileCheck style={{ width: '18px', height: '18px', color: '#34d399' }} />
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#34d399', marginTop: '6px' }}>
                {d.documents?.totalDocuments ?? 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                Indexed Chunks: <strong>{d.documents?.totalIndexedChunks ?? 0}</strong>
              </div>
            </div>

          </div>

          {/* Quick Action Cards & Common Skill Gaps */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' }}>
            
            {/* Top Common Industry Skill Gaps */}
            <div className="glass-card" style={{ padding: '22px' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Layers style={{ width: '18px', height: '18px', color: '#fb923c' }} /> Top Missing Skills Across Student Cohort
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {d.career?.commonSkillGaps?.map((item, idx) => (
                  <div 
                    key={idx}
                    style={{
                      padding: '10px 14px',
                      borderRadius: '8px',
                      background: 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid var(--border-color)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                  >
                    <span style={{ fontWeight: 600, fontSize: '0.9rem', color: '#f8fafc' }}>{item.skill}</span>
                    <span className="badge badge-rose">{item.count} student(s)</span>
                  </div>
                ))}
                {(!d.career?.commonSkillGaps || d.career.commonSkillGaps.length === 0) && (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No student skill gap records yet.</p>
                )}
              </div>
            </div>

            {/* Quick Action Navigation */}
            <div className="glass-card" style={{ padding: '22px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Sparkles style={{ width: '18px', height: '18px', color: '#f59e0b' }} /> Administrative Quick Actions
                </h3>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px', lineHeight: '1.5' }}>
                  Access student records, evaluate critical academic alerts, review semantic RAG embeddings, or run Gemini AI executive advisory summaries.
                </p>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <button className="btn btn-secondary" onClick={() => setActiveTab('students')}>
                  <Users style={{ width: '15px', height: '15px' }} /> View Student Directory
                </button>
                <button className="btn btn-secondary" onClick={() => setActiveTab('risks')}>
                  <AlertTriangle style={{ width: '15px', height: '15px' }} /> View At-Risk Students
                </button>
                <button className="btn btn-primary" onClick={() => { setActiveTab('insights'); handleGenerateInsights(); }}>
                  <Brain style={{ width: '15px', height: '15px' }} /> Run AI Insights
                </button>
                <button className="btn btn-secondary" onClick={() => setActiveTab('documents')}>
                  <FileText style={{ width: '15px', height: '15px' }} /> Manage Documents
                </button>
              </div>
            </div>

          </div>

        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: STUDENT MANAGEMENT & DIRECTORY                                     */}
      {/* ========================================================================= */}
      {activeTab === 'students' && (
        <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          
          {/* Search & Filter Bar */}
          <div className="glass-card" style={{ padding: '18px', display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
            <div style={{ flex: '1 1 240px', position: 'relative' }}>
              <Search style={{ width: '16px', height: '16px', position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input 
                type="text" 
                className="input-field" 
                style={{ paddingLeft: '36px' }}
                placeholder="Search students by name, email, code, or department..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />
            </div>

            <select 
              className="input-field" 
              style={{ width: '140px' }}
              value={semesterFilter}
              onChange={e => setSemesterFilter(e.target.value)}
            >
              <option value="">All Semesters</option>
              <option value="1">Semester 1</option>
              <option value="2">Semester 2</option>
              <option value="3">Semester 3</option>
              <option value="4">Semester 4</option>
              <option value="5">Semester 5</option>
              <option value="6">Semester 6</option>
              <option value="7">Semester 7</option>
              <option value="8">Semester 8</option>
            </select>

            <select 
              className="input-field" 
              style={{ width: '180px' }}
              value={riskFilter}
              onChange={e => setRiskFilter(e.target.value)}
            >
              <option value="">All Attendance Risks</option>
              <option value="CRITICAL">Critical (&lt;75%)</option>
              <option value="WARNING">Warning (75-80%)</option>
              <option value="SAFE">Safe (&ge;80%)</option>
            </select>

            {(searchQuery || semesterFilter || departmentFilter || riskFilter) && (
              <button 
                className="btn btn-secondary btn-sm"
                onClick={() => { setSearchQuery(''); setSemesterFilter(''); setDepartmentFilter(''); setRiskFilter(''); }}
              >
                Clear Filters
              </button>
            )}
          </div>

          {/* Student Table */}
          <div className="glass-card" style={{ padding: '0px', overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', background: 'rgba(255, 255, 255, 0.02)' }}>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Student</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Department / Sem</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Attendance</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Assignments</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Exams</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Quiz Avg</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Risk Flags</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)', textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {students.map(s => (
                  <tr key={s.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 700, color: '#f8fafc' }}>{s.name}</div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{s.email} • {s.studentCode}</div>
                    </td>

                    <td style={{ padding: '14px 16px' }}>
                      <div>{s.department}</div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Sem {s.semester} • GPA {s.gpa}</div>
                    </td>

                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 700, color: s.attendance.percentage >= 75 ? '#34d399' : '#f87171' }}>
                        {s.attendance.percentage}%
                      </div>
                      <span className={`badge ${s.attendance.riskClassification === 'SAFE' ? 'badge-emerald' : s.attendance.riskClassification === 'WARNING' ? 'badge-amber' : 'badge-rose'}`} style={{ fontSize: '0.68rem' }}>
                        {s.attendance.riskClassification}
                      </span>
                    </td>

                    <td style={{ padding: '14px 16px' }}>
                      <div>Pending: <strong>{s.pendingAssignmentsCount}</strong></div>
                      {s.overdueAssignmentsCount > 0 && (
                        <div style={{ color: '#f87171', fontSize: '0.75rem' }}>Overdue: {s.overdueAssignmentsCount}</div>
                      )}
                    </td>

                    <td style={{ padding: '14px 16px' }}>
                      <div>Upcoming: <strong>{s.upcomingExamsCount}</strong></div>
                    </td>

                    <td style={{ padding: '14px 16px' }}>
                      {s.averageQuizScore !== null ? (
                        <span style={{ fontWeight: 700, color: s.averageQuizScore >= 60 ? '#67e8f9' : '#f87171' }}>
                          {s.averageQuizScore}%
                        </span>
                      ) : (
                        <span style={{ color: 'var(--text-muted)' }}>N/A</span>
                      )}
                    </td>

                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                        {s.riskIndicators.map((r, i) => (
                          <span key={i} className="badge badge-rose" style={{ fontSize: '0.65rem' }}>
                            {r.replace('_', ' ')}
                          </span>
                        ))}
                        {s.riskIndicators.length === 0 && (
                          <span className="badge badge-emerald" style={{ fontSize: '0.68rem' }}>Good Standing</span>
                        )}
                      </div>
                    </td>

                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <button 
                        className="btn btn-secondary btn-sm"
                        onClick={() => handleOpenStudentDetail(s.id)}
                        style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                      >
                        <Eye style={{ width: '13px', height: '13px' }} /> View
                      </button>
                    </td>
                  </tr>
                ))}

                {students.length === 0 && (
                  <tr>
                    <td colSpan={8} style={{ padding: '36px', textAlign: 'center', color: 'var(--text-muted)' }}>
                      No students found matching current filters.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 3: ACADEMIC RISK INTELLIGENCE                                         */}
      {/* ========================================================================= */}
      {activeTab === 'risks' && (
        <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
            
            {/* 1. Attendance Risk Card */}
            <div className="glass-card" style={{ padding: '22px', border: '1px solid rgba(244, 63, 94, 0.3)' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#fca5a5', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <AlertCircle style={{ width: '18px', height: '18px' }} /> Attendance Risk (&lt; 75%)
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {riskData?.attendanceRisk?.map((s, idx) => (
                  <div key={idx} style={{ padding: '12px', borderRadius: '8px', background: 'rgba(244, 63, 94, 0.06)', border: '1px solid rgba(244, 63, 94, 0.2)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <strong style={{ color: '#f8fafc' }}>{s.name}</strong>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{s.department} • Sem {s.semester}</div>
                    </div>
                    <span className="badge badge-rose">{s.percentage}%</span>
                  </div>
                ))}
                {(!riskData?.attendanceRisk || riskData.attendanceRisk.length === 0) && (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>✓ All students meet the 75% attendance rule.</p>
                )}
              </div>
            </div>

            {/* 2. Assignment Risk Card */}
            <div className="glass-card" style={{ padding: '22px', border: '1px solid rgba(251, 146, 60, 0.3)' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#fdba74', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Clock style={{ width: '18px', height: '18px' }} /> Overdue Assignment Submissions
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {riskData?.assignmentRisk?.map((s, idx) => (
                  <div key={idx} style={{ padding: '12px', borderRadius: '8px', background: 'rgba(251, 146, 60, 0.06)', border: '1px solid rgba(251, 146, 60, 0.2)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <strong style={{ color: '#f8fafc' }}>{s.name}</strong>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{s.department}</div>
                    </div>
                    <span className="badge badge-amber">{s.overdueCount} Overdue</span>
                  </div>
                ))}
                {(!riskData?.assignmentRisk || riskData.assignmentRisk.length === 0) && (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>✓ No overdue assignments recorded.</p>
                )}
              </div>
            </div>

            {/* 3. Quiz Performance Risk Card */}
            <div className="glass-card" style={{ padding: '22px', border: '1px solid rgba(168, 85, 247, 0.3)' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#d8b4fe', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Award style={{ width: '18px', height: '18px' }} /> Academic Quiz Gaps (&lt; 60%)
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {riskData?.quizRisk?.map((s, idx) => (
                  <div key={idx} style={{ padding: '12px', borderRadius: '8px', background: 'rgba(168, 85, 247, 0.06)', border: '1px solid rgba(168, 85, 247, 0.2)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <strong style={{ color: '#f8fafc' }}>{s.name}</strong>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{s.department}</div>
                    </div>
                    <span className="badge badge-rose">{s.averageScore}%</span>
                  </div>
                ))}
                {(!riskData?.quizRisk || riskData.quizRisk.length === 0) && (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>✓ No critical quiz weaknesses detected.</p>
                )}
              </div>
            </div>

            {/* 4. Career Skill Gaps Card */}
            <div className="glass-card" style={{ padding: '22px', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#7dd3fc', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Layers style={{ width: '18px', height: '18px' }} /> Major Career Competency Gaps
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {riskData?.careerGap?.map((s, idx) => (
                  <div key={idx} style={{ padding: '12px', borderRadius: '8px', background: 'rgba(56, 189, 248, 0.06)', border: '1px solid rgba(56, 189, 248, 0.2)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <strong style={{ color: '#f8fafc' }}>{s.name}</strong>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Role: {s.targetRole}</div>
                    </div>
                    <span className="badge badge-cyan">{s.skillCoverage}% Coverage</span>
                  </div>
                ))}
                {(!riskData?.careerGap || riskData.careerGap.length === 0) && (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>✓ All career profiles meet competency baselines.</p>
                )}
              </div>
            </div>

          </div>

        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 4: AI EXECUTIVE INSIGHTS                                              */}
      {/* ========================================================================= */}
      {activeTab === 'insights' && (
        <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#fde047', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Brain style={{ width: '22px', height: '22px' }} /> Gemini AI Executive Intelligence Advisor
            </h3>
            <button 
              className="btn btn-primary"
              disabled={loadingInsights}
              onClick={handleGenerateInsights}
              style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Sparkles style={{ width: '16px', height: '16px' }} />
              {loadingInsights ? 'Analyzing Campus Metrics...' : 'Re-Run Executive Analysis'}
            </button>
          </div>

          {insights && (
            <div className="glass-card animate-fade-in" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px', background: 'linear-gradient(135deg, rgba(30, 27, 75, 0.95), rgba(15, 23, 42, 0.98))', border: '1px solid rgba(245, 158, 11, 0.3)' }}>
              
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span className="badge badge-amber">{insights.isAiGenerated ? 'Gemini 2.5 Live Executive Advisory' : 'Deterministic Advisory Fallback'}</span>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Strictly Factual • Non-destructive</span>
              </div>

              {/* Observations & Attention Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '18px' }}>
                
                <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(56, 189, 248, 0.05)', border: '1px solid rgba(56, 189, 248, 0.2)' }}>
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#38bdf8', marginBottom: '10px' }}>
                    📌 Key Academic Observations
                  </h4>
                  <ul style={{ paddingLeft: '18px', fontSize: '0.85rem', color: '#cbd5e1', lineHeight: '1.6' }}>
                    {insights.keyObservations?.map((o, i) => <li key={i}>{o}</li>)}
                  </ul>
                </div>

                <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(244, 63, 94, 0.05)', border: '1px solid rgba(244, 63, 94, 0.2)' }}>
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#fca5a5', marginBottom: '10px' }}>
                    ⚠️ Areas Requiring Administrative Attention
                  </h4>
                  <ul style={{ paddingLeft: '18px', fontSize: '0.85rem', color: '#cbd5e1', lineHeight: '1.6' }}>
                    {insights.areasRequiringAttention?.map((a, i) => <li key={i}>{a}</li>)}
                  </ul>
                </div>

              </div>

              {/* Actionable Recommendations */}
              <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(52, 211, 153, 0.05)', border: '1px solid rgba(52, 211, 153, 0.2)' }}>
                <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#34d399', marginBottom: '10px' }}>
                  🎯 Suggested Administrative Actions
                </h4>
                <ul style={{ paddingLeft: '18px', fontSize: '0.88rem', color: '#e2e8f0', lineHeight: '1.6' }}>
                  {insights.suggestedAdministrativeActions?.map((act, i) => <li key={i}>{act}</li>)}
                </ul>
              </div>

              {insights.caveatsAndLimitations && (
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontStyle: 'italic', borderTop: '1px solid var(--border-color)', paddingTop: '10px' }}>
                  * {insights.caveatsAndLimitations}
                </div>
              )}

            </div>
          )}

        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 5: DOCUMENT / RAG ADMIN MANAGEMENT                                    */}
      {/* ========================================================================= */}
      {activeTab === 'documents' && (
        <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <FileCheck style={{ width: '20px', height: '20px', color: '#34d399' }} /> College RAG Knowledge Base Governance
            </h3>
            <span className="badge badge-emerald">Model: {activeEmbeddingModel} (768-dim)</span>
          </div>

          <div className="glass-card" style={{ padding: '0px', overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', background: 'rgba(255, 255, 255, 0.02)' }}>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Document Title</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>File Name</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Pages / Chunks</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>File Size</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Embedding Status</th>
                  <th style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>Uploaded</th>
                </tr>
              </thead>
              <tbody>
                {documents.map(doc => (
                  <tr key={doc.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: '#f8fafc' }}>{doc.title}</td>
                    <td style={{ padding: '14px 16px', color: 'var(--text-muted)', fontFamily: 'monospace' }}>{doc.fileName}</td>
                    <td style={{ padding: '14px 16px' }}>{doc.pageCount} page(s) • <strong>{doc.chunkCount}</strong> chunks</td>
                    <td style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>{(doc.fileSize / 1024).toFixed(1)} KB</td>
                    <td style={{ padding: '14px 16px' }}>
                      <span className={`badge ${doc.embeddingStatus === 'Indexed' ? 'badge-emerald' : 'badge-amber'}`}>
                        {doc.embeddingStatus}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px', color: 'var(--text-muted)' }}>
                      {new Date(doc.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 6: PRESERVED COURSE & ANNOUNCEMENT MANAGEMENT                         */}
      {/* ========================================================================= */}
      {activeTab === 'manage' && (
        <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
            <button className="btn btn-secondary" onClick={() => setShowSubjectModal(true)}>
              <Plus style={{ width: '16px', height: '16px' }} /> Add Course Subject
            </button>
            <button className="btn btn-primary" onClick={() => setShowAncModal(true)}>
              <Megaphone style={{ width: '16px', height: '16px' }} /> Post Campus Announcement
            </button>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' }}>
            {/* Announcements List */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Megaphone style={{ width: '18px', height: '18px', color: '#f59e0b' }} /> Campus Announcements
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {legacyStats?.recentAnnouncements?.map((anc) => (
                  <div key={anc.id} style={{ padding: '12px', borderRadius: '8px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)' }}>
                    <div style={{ fontWeight: 700, fontSize: '0.92rem', color: '#fde047' }}>{anc.title}</div>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: '4px' }}>{anc.content}</div>
                  </div>
                ))}
              </div>
            </div>

            {/* FAQs List */}
            <div className="glass-card" style={{ padding: '20px' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <HelpCircle style={{ width: '18px', height: '18px', color: '#38bdf8' }} /> Student FAQs
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {legacyStats?.faqs?.map((f) => (
                  <div key={f.id} style={{ padding: '12px', borderRadius: '8px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)' }}>
                    <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#67e8f9' }}>Q: {f.question}</div>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: '4px' }}>A: {f.answer}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>

        </div>
      )}

      {/* ========================================================================= */}
      {/* STUDENT DETAIL DRILL-DOWN MODAL                                           */}
      {/* ========================================================================= */}
      {selectedStudentId && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(5px)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '20px' }}>
          <div className="glass-card animate-fade-in" style={{ width: '100%', maxWidth: '850px', maxHeight: '90vh', overflowY: 'auto', padding: '28px', position: 'relative', border: '1px solid #38bdf8' }}>
            
            <button 
              onClick={() => setSelectedStudentId(null)}
              style={{ position: 'absolute', top: '20px', right: '20px', background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
            >
              <X style={{ width: '20px', height: '20px' }} />
            </button>

            {loadingDetail ? (
              <div style={{ textAlign: 'center', padding: '40px' }}>⏳ Loading comprehensive student academic record...</div>
            ) : studentDetail ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                
                {/* Header Profile */}
                <div style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '16px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '8px' }}>
                    <div>
                      <h3 style={{ fontSize: '1.3rem', fontWeight: 800, color: '#f8fafc' }}>{studentDetail.profile.name}</h3>
                      <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{studentDetail.profile.email} • Code: {studentDetail.profile.studentCode}</p>
                    </div>
                    <span className="badge badge-cyan">{studentDetail.profile.department} (Sem {studentDetail.profile.semester})</span>
                  </div>
                </div>

                {/* Attendance Summary */}
                <div>
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#34d399', marginBottom: '8px' }}>
                    📊 Overall Attendance: {studentDetail.attendance.overall.percentage}% ({studentDetail.attendance.overall.riskClassification})
                  </h4>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '10px' }}>
                    {studentDetail.attendance.subjects.map(s => (
                      <div key={s.subjectId} style={{ padding: '10px', borderRadius: '8px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)', fontSize: '0.82rem' }}>
                        <strong>{s.subjectName}</strong> ({s.subjectCode})
                        <div style={{ color: s.percentage >= 75 ? '#34d399' : '#f87171', fontWeight: 700, marginTop: '2px' }}>
                          {s.percentage}% ({s.attended}/{s.conducted})
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Assignments & Exams */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div style={{ padding: '14px', borderRadius: '8px', background: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)' }}>
                    <h5 style={{ fontWeight: 700, fontSize: '0.88rem', color: '#fde047', marginBottom: '8px' }}>Assignments ({studentDetail.assignments.total})</h5>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Pending: <strong>{studentDetail.assignments.pending}</strong> | Overdue: <strong style={{ color: '#f87171' }}>{studentDetail.assignments.overdue}</strong></div>
                  </div>

                  <div style={{ padding: '14px', borderRadius: '8px', background: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)' }}>
                    <h5 style={{ fontWeight: 700, fontSize: '0.88rem', color: '#a78bfa', marginBottom: '8px' }}>Scheduled Exams ({studentDetail.exams.total})</h5>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Upcoming: <strong>{studentDetail.exams.upcoming}</strong> | Today: <strong>{studentDetail.exams.today}</strong></div>
                  </div>
                </div>

                {/* Career & Resumes */}
                {studentDetail.career && (
                  <div style={{ padding: '14px', borderRadius: '8px', background: 'rgba(56, 189, 248, 0.04)', border: '1px solid rgba(56, 189, 248, 0.2)' }}>
                    <h5 style={{ fontWeight: 700, fontSize: '0.88rem', color: '#38bdf8', marginBottom: '6px' }}>Career Target: {studentDetail.career.targetRole}</h5>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '8px' }}>
                      Skill Gap Coverage: <strong>{studentDetail.career.skillGapAnalysis.coveragePercentage}%</strong>
                    </div>
                    <div style={{ fontSize: '0.78rem' }}>
                      <span style={{ color: '#fca5a5' }}>Identified Gaps: </span>
                      {studentDetail.career.skillGapAnalysis.missingList?.join(', ') || 'None'}
                    </div>
                  </div>
                )}

              </div>
            ) : null}

          </div>
        </div>
      )}

      {/* Course Subject Modal (Preserved) */}
      {showSubjectModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '20px' }}>
          <div className="glass-card" style={{ width: '100%', maxWidth: '440px', padding: '24px' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '16px' }}>Add Course Subject</h3>
            <form onSubmit={handleCreateSubject} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <input type="text" placeholder="Course Code (e.g. CS 401)" className="input-field" required value={code} onChange={e=>setCode(e.target.value)} />
              <input type="text" placeholder="Subject Name (e.g. Distributed Systems)" className="input-field" required value={name} onChange={e=>setName(e.target.value)} />
              <input type="text" placeholder="Instructor Name" className="input-field" value={instructor} onChange={e=>setInstructor(e.target.value)} />
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowSubjectModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Course</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Announcement Modal (Preserved) */}
      {showAncModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '20px' }}>
          <div className="glass-card" style={{ width: '100%', maxWidth: '480px', padding: '24px' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '16px' }}>Post Campus Announcement</h3>
            <form onSubmit={handleCreateAnc} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <input type="text" placeholder="Announcement Title" className="input-field" required value={ancTitle} onChange={e=>setAncTitle(e.target.value)} />
              <textarea placeholder="Announcement Body Content..." rows={4} className="input-field" required value={ancContent} onChange={e=>setAncContent(e.target.value)} />
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAncModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Publish Announcement</button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
