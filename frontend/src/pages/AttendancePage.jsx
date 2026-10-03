import React, { useState, useEffect } from 'react';
import { 
  CheckCircle2, 
  AlertTriangle, 
  Calculator, 
  Sparkles, 
  TrendingUp, 
  Percent, 
  ShieldCheck,
  Brain,
  Info
} from 'lucide-react';
import api from '../services/api';

export default function AttendancePage() {
  const [attendance, setAttendance] = useState([]);
  const [loading, setLoading] = useState(true);
  const [targetPercentage, setTargetPercentage] = useState(75);

  // Calculator State
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [missCount, setMissCount] = useState('2');
  const [attendCount, setAttendCount] = useState('3');
  const [projectionResult, setProjectionResult] = useState(null);

  // AI Explanation State
  const [aiExplanation, setAiExplanation] = useState(null);
  const [explainingLoading, setExplainingLoading] = useState(false);

  const fetchAttendance = async (target = targetPercentage) => {
    try {
      const res = await api.get(`/attendance?target=${target}`);
      if (res.data.success) {
        setAttendance(res.data.data);
        if (res.data.data[0] && !selectedSubjectId) {
          setSelectedSubjectId(res.data.data[0].id);
        }
      }
    } catch (err) {
      console.warn('Attendance fetch fallback');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAttendance(targetPercentage);
  }, [targetPercentage]);

  const handleCalculateProjection = async () => {
    const target = attendance.find(a => a.id === selectedSubjectId || a.subjectId === selectedSubjectId) || attendance[0];
    if (!target) return;

    try {
      const res = await api.post('/attendance/projection', {
        conducted: target.conducted,
        attended: target.attended,
        missClasses: Number(missCount),
        attendClasses: Number(attendCount),
        targetPercentage
      });
      if (res.data.success) {
        setProjectionResult(res.data.data);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleFetchAiExplanation = async (subj) => {
    const targetObj = subj || attendance.find(a => a.id === selectedSubjectId || a.subjectId === selectedSubjectId) || attendance[0];
    if (!targetObj) return;

    setExplainingLoading(true);
    setAiExplanation(null);

    try {
      const res = await api.post('/attendance/explain', {
        subjectId: targetObj.subjectId || targetObj.id,
        conducted: targetObj.conducted,
        attended: targetObj.attended,
        targetPercentage
      });
      if (res.data.success) {
        setAiExplanation(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setExplainingLoading(false);
    }
  };

  const getRiskBadge = (risk, percentage) => {
    if (risk === 'CRITICAL' || percentage < 75) {
      return <span className="badge badge-rose" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><AlertTriangle style={{ width: '12px', height: '12px' }} /> Critical (&lt;75%)</span>;
    }
    if (risk === 'WARNING' || (percentage >= 75 && percentage < 80)) {
      return <span className="badge badge-amber" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><Info style={{ width: '12px', height: '12px' }} /> At Risk (75-80%)</span>;
    }
    return <span className="badge badge-emerald" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><ShieldCheck style={{ width: '12px', height: '12px' }} /> Safe (&ge;80%)</span>;
  };

  if (loading) return <div className="glass-card" style={{ padding: '60px', textAlign: 'center' }}>⏳ Loading Attendance Intelligence...</div>;

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header bar */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Percent style={{ width: '22px', height: '22px', color: '#6ee7b7' }} /> Attendance Intelligence & Projection
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Deterministic attendance analysis, target calculations, future projections, and AI guidance.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <label style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>Target Threshold:</label>
          <select 
            className="input-field" 
            style={{ width: '90px', padding: '6px 10px', fontSize: '0.9rem' }}
            value={targetPercentage} 
            onChange={(e) => setTargetPercentage(Number(e.target.value))}
          >
            <option value={75}>75%</option>
            <option value={80}>80%</option>
            <option value={85}>85%</option>
            <option value={90}>90%</option>
          </select>
        </div>
      </div>

      {/* Attendance Cards Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '16px' }}>
        {attendance.map((item) => (
          <div key={item.id} className="glass-card" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <span className="badge badge-cyan">{item.subjectCode}</span>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginTop: '4px' }}>{item.subjectName}</h3>
              </div>
              <span className={`badge ${item.percentage >= targetPercentage ? 'badge-emerald' : 'badge-rose'}`} style={{ fontSize: '1rem', fontWeight: 800 }}>
                {item.percentage}%
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', color: 'var(--text-muted)' }}>
              <span>Conducted: <strong>{item.conducted}</strong></span>
              <span>Attended: <strong>{item.attended}</strong></span>
            </div>

            {/* Progress bar */}
            <div style={{ width: '100%', height: '8px', background: 'rgba(255, 255, 255, 0.08)', borderRadius: '4px', overflow: 'hidden' }}>
              <div style={{ width: `${Math.min(100, item.percentage)}%`, height: '100%', background: item.percentage >= targetPercentage ? '#10b981' : '#f43f5e', borderRadius: '4px' }} />
            </div>

            {/* Metrics Breakdown */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', background: 'rgba(255,255,255,0.03)', padding: '10px', borderRadius: '8px', fontSize: '0.8rem' }}>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Max Can Miss:</span>
                <strong style={{ color: item.maxCanMiss > 0 ? '#6ee7b7' : 'var(--text-muted)', fontSize: '0.95rem' }}>
                  {item.maxCanMiss} classes
                </strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block' }}>Must Attend:</span>
                <strong style={{ color: item.minRequiredToAttend > 0 ? '#f43f5e' : '#6ee7b7', fontSize: '0.95rem' }}>
                  {item.minRequiredToAttend} consecutive
                </strong>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '4px' }}>
              {getRiskBadge(item.riskClassification, item.percentage)}

              <button 
                className="btn" 
                style={{ padding: '4px 10px', fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: '4px', background: 'rgba(99, 102, 241, 0.15)', color: '#818cf8', border: '1px solid rgba(99, 102, 241, 0.3)' }}
                onClick={() => handleFetchAiExplanation(item)}
              >
                <Brain style={{ width: '14px', height: '14px' }} /> AI Insight
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* AI Explanation Banner / Drawer */}
      {explainingLoading && (
        <div className="glass-card" style={{ padding: '20px', textAlign: 'center', color: '#818cf8' }}>
          <Sparkles className="animate-spin" style={{ display: 'inline-block', marginRight: '8px' }} />
          Generating Gemini AI Attendance Breakdown...
        </div>
      )}

      {aiExplanation && (
        <div className="glass-card" style={{ padding: '20px', borderLeft: '4px solid #818cf8', background: 'rgba(99, 102, 241, 0.05)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#818cf8', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Brain style={{ width: '18px', height: '18px' }} /> Gemini AI Attendance Advisory
            </h3>
            {aiExplanation.isFallback && <span className="badge badge-amber" style={{ fontSize: '0.75rem' }}>Deterministic Analysis</span>}
          </div>
          <p style={{ fontSize: '0.9rem', lineHeight: '1.5', color: 'var(--text-main)' }}>
            {aiExplanation.explanation}
          </p>
        </div>
      )}

      {/* Projection Calculator Panel */}
      <div className="glass-card" style={{ padding: '24px' }}>
        <h3 style={{ fontSize: '1.15rem', fontWeight: 800, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Calculator style={{ width: '20px', height: '20px', color: '#818cf8' }} /> Attendance Projection Calculator
        </h3>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '14px', marginBottom: '20px' }}>
          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Select Subject</label>
            <select className="input-field" value={selectedSubjectId} onChange={(e) => setSelectedSubjectId(e.target.value)}>
              {attendance.map(a => <option key={a.id || a.subjectId} value={a.id || a.subjectId}>{a.subjectCode} - {a.subjectName}</option>)}
            </select>
          </div>

          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>If I MISS next X classes:</label>
            <input type="number" min="0" className="input-field" value={missCount} onChange={(e) => setMissCount(e.target.value)} />
          </div>

          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>If I ATTEND next X classes:</label>
            <input type="number" min="0" className="input-field" value={attendCount} onChange={(e) => setAttendCount(e.target.value)} />
          </div>
        </div>

        <button className="btn btn-primary" onClick={handleCalculateProjection}>
          Calculate Projections
        </button>

        {/* Projection Results */}
        {projectionResult && (
          <div style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px solid var(--border-color)', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(244, 63, 94, 0.08)', border: '1px solid rgba(244, 63, 94, 0.2)' }}>
              <div style={{ fontSize: '0.85rem', color: '#fca5a5', fontWeight: 700 }}>Scenario 1: Miss next {projectionResult.ifMissNext.additionalMissed} classes</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, margin: '6px 0', color: projectionResult.ifMissNext.projectedPercentage >= targetPercentage ? '#6ee7b7' : '#f43f5e' }}>
                {projectionResult.ifMissNext.projectedPercentage}%
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                ({projectionResult.ifMissNext.projectedAttended} / {projectionResult.ifMissNext.projectedConducted} classes)
              </div>
            </div>

            <div style={{ padding: '16px', borderRadius: '10px', background: 'rgba(16, 185, 129, 0.08)', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
              <div style={{ fontSize: '0.85rem', color: '#6ee7b7', fontWeight: 700 }}>Scenario 2: Attend next {projectionResult.ifAttendNext.additionalAttended} classes</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, margin: '6px 0', color: '#6ee7b7' }}>
                {projectionResult.ifAttendNext.projectedPercentage}%
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                ({projectionResult.ifAttendNext.projectedAttended} / {projectionResult.ifAttendNext.projectedConducted} classes)
              </div>
            </div>
            
            <p style={{ gridColumn: '1 / -1', fontSize: '0.78rem', color: 'var(--text-dim)', marginTop: '4px' }}>
              ⚠️ {projectionResult.disclaimer}
            </p>
          </div>
        )}
      </div>

    </div>
  );
}
