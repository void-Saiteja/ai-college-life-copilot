import React, { useState, useEffect } from 'react';
import { 
  Clock, 
  Calendar, 
  Plus, 
  Trash2, 
  MapPin, 
  User, 
  BookOpen, 
  CheckCircle2, 
  Sparkles 
} from 'lucide-react';
import api from '../services/api';

const DAYS_OF_WEEK = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'];

export default function SchedulePage() {
  const [schedule, setSchedule] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDay, setSelectedDay] = useState('All');
  const [showModal, setShowModal] = useState(false);

  // New Class Form State
  const [course, setCourse] = useState('');
  const [room, setRoom] = useState('');
  const [time, setTime] = useState('');
  const [instructor, setInstructor] = useState('');
  const [selectedDays, setSelectedDays] = useState(['Mon', 'Wed', 'Fri']);

  const currentDayName = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][new Date().getDay()];

  const fetchSchedule = async () => {
    try {
      const res = await api.get('/schedule');
      if (res.data.success) {
        setSchedule(res.data.data || []);
      }
    } catch (err) {
      console.warn('Schedule fallback');
      setSchedule([
        { id: '1', course: 'CS 201: Data Structures', room: 'Hall B2', days: ['Mon', 'Wed', 'Fri'], time: '09:00 AM - 10:30 AM', instructor: 'Dr. Alan Turing' },
        { id: '2', course: 'MATH 220: Calculus III', room: 'Sci 104', days: ['Tue', 'Thu'], time: '11:00 AM - 12:30 PM', instructor: 'Prof. Katherine Johnson' },
        { id: '3', course: 'CS 340: Database Systems', room: 'Lab 4', days: ['Mon', 'Wed'], time: '02:00 PM - 03:30 PM', instructor: 'Dr. Grace Hopper' }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSchedule();
  }, []);

  const handleAddClass = async (e) => {
    e.preventDefault();
    if (!course || !time || selectedDays.length === 0) return;

    try {
      const res = await api.post('/schedule', {
        course,
        room: room || 'Lecture Hall A',
        days: selectedDays,
        time,
        instructor: instructor || 'Faculty Staff'
      });
      if (res.data.success) {
        fetchSchedule();
        setCourse('');
        setRoom('');
        setTime('');
        setInstructor('');
        setSelectedDays(['Mon', 'Wed', 'Fri']);
        setShowModal(false);
      }
    } catch (err) {
      console.error('Failed to add schedule item', err);
    }
  };

  const handleDeleteClass = async (id) => {
    try {
      const res = await api.delete(`/schedule/${id}`);
      if (res.data.success) {
        fetchSchedule();
      }
    } catch (err) {
      console.error('Failed to delete schedule item', err);
    }
  };

  const toggleDaySelection = (day) => {
    if (selectedDays.includes(day)) {
      if (selectedDays.length > 1) {
        setSelectedDays(selectedDays.filter(d => d !== day));
      }
    } else {
      setSelectedDays([...selectedDays, day]);
    }
  };

  const filteredSchedule = schedule.filter(item => {
    if (selectedDay === 'All') return true;
    if (Array.isArray(item.days)) {
      return item.days.includes(selectedDay);
    }
    return item.days === selectedDay;
  });

  const todayClasses = schedule.filter(item => {
    if (Array.isArray(item.days)) {
      return item.days.includes(currentDayName);
    }
    return item.days === currentDayName;
  });

  if (loading) {
    return (
      <div className="glass-card" style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}>
        ⏳ Loading your semester timetable & class schedules...
      </div>
    );
  }

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock style={{ width: '22px', height: '22px', color: '#6366f1' }} /> Class Timetable & Schedule
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Keep track of lecture halls, professor office hours, and daily campus periods.
          </p>
        </div>

        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          <Plus style={{ width: '16px', height: '16px' }} /> Add Lecture / Lab
        </button>
      </div>

      {/* Metrics Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Total Weekly Classes</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#f8fafc', marginTop: '4px' }}>
            {schedule.length}
          </div>
          <div style={{ fontSize: '0.78rem', color: '#818cf8', marginTop: '4px' }}>Across registered semester subjects</div>
        </div>

        <div className="glass-card" style={{ padding: '20px', background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(15, 23, 42, 0.8))' }}>
          <div style={{ fontSize: '0.82rem', color: '#a5b4fc' }}>Today's Classes ({currentDayName})</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#c7d2fe', marginTop: '4px' }}>
            {todayClasses.length}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            {todayClasses.length > 0 ? 'Upcoming lectures on schedule today' : 'No classes scheduled today!'}
          </div>
        </div>
      </div>

      {/* Day Filter Bar */}
      <div className="glass-card" style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
        <span style={{ fontSize: '0.85rem', fontWeight: 700, marginRight: '8px', color: 'var(--text-muted)' }}>Filter Day:</span>
        <button
          className={`btn btn-sm ${selectedDay === 'All' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setSelectedDay('All')}
        >
          All Days
        </button>
        {DAYS_OF_WEEK.map(day => (
          <button
            key={day}
            className={`btn btn-sm ${selectedDay === day ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setSelectedDay(day)}
            style={{
              position: 'relative'
            }}
          >
            {day}
            {day === currentDayName && (
              <span style={{
                position: 'absolute',
                top: '-4px',
                right: '-4px',
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                backgroundColor: '#10b981'
              }} />
            )}
          </button>
        ))}
      </div>

      {/* Classes Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '16px' }}>
        {filteredSchedule.length === 0 ? (
          <div className="glass-card" style={{ gridColumn: '1 / -1', padding: '50px', textAlign: 'center', color: 'var(--text-muted)' }}>
            No lectures scheduled for {selectedDay === 'All' ? 'any day' : selectedDay}. Click "+ Add Lecture / Lab" to add one!
          </div>
        ) : (
          filteredSchedule.map(item => {
            const isToday = Array.isArray(item.days) && item.days.includes(currentDayName);
            return (
              <div 
                key={item.id} 
                className="glass-card glass-card-interactive" 
                style={{ 
                  padding: '20px', 
                  display: 'flex', 
                  flexDirection: 'column', 
                  gap: '14px',
                  borderLeft: isToday ? '4px solid #10b981' : '1px solid var(--border-color)'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    {isToday && (
                      <span className="badge badge-emerald" style={{ marginBottom: '6px', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <Sparkles style={{ width: '10px', height: '10px' }} /> Scheduled Today
                      </span>
                    )}
                    <h3 style={{ fontSize: '1.1rem', fontWeight: 800 }}>{item.course}</h3>
                  </div>
                  <button 
                    className="btn btn-secondary btn-sm" 
                    onClick={() => handleDeleteClass(item.id)} 
                    style={{ color: '#fca5a5' }}
                    title="Remove class"
                  >
                    <Trash2 style={{ width: '14px', height: '14px' }} />
                  </button>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '0.85rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#cbd5e1' }}>
                    <Clock style={{ width: '15px', height: '15px', color: '#818cf8' }} />
                    <span style={{ fontWeight: 600 }}>{item.time}</span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)' }}>
                    <MapPin style={{ width: '15px', height: '15px', color: '#ec4899' }} />
                    <span>Room: <strong>{item.room}</strong></span>
                  </div>

                  {item.instructor && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)' }}>
                      <User style={{ width: '15px', height: '15px', color: '#38bdf8' }} />
                      <span>{item.instructor}</span>
                    </div>
                  )}
                </div>

                {/* Days of week pills */}
                <div style={{ marginTop: 'auto', paddingTop: '10px', borderTop: '1px solid var(--border-color)', display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                  {DAYS_OF_WEEK.map(d => {
                    const active = Array.isArray(item.days) ? item.days.includes(d) : item.days === d;
                    return (
                      <span 
                        key={d} 
                        style={{
                          fontSize: '0.72rem',
                          fontWeight: 700,
                          padding: '3px 8px',
                          borderRadius: '6px',
                          background: active ? 'linear-gradient(135deg, rgba(99, 102, 241, 0.4), rgba(139, 92, 246, 0.3))' : 'rgba(255, 255, 255, 0.03)',
                          color: active ? '#ffffff' : 'rgba(255, 255, 255, 0.25)',
                          border: active ? '1px solid rgba(99, 102, 241, 0.4)' : '1px solid transparent'
                        }}
                      >
                        {d}
                      </span>
                    );
                  })}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Modal to Add Class */}
      {showModal && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.75)',
          backdropFilter: 'blur(8px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 100
        }}>
          <div className="glass-card" style={{ width: '100%', maxWidth: '500px', padding: '28px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '16px' }}>Add Lecture / Lab Class</h3>

            <form onSubmit={handleAddClass} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Course Title & Code</label>
                <input 
                  type="text" 
                  className="input-field" 
                  required 
                  placeholder="e.g. CS 410: Artificial Intelligence" 
                  value={course} 
                  onChange={(e) => setCourse(e.target.value)} 
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Time Slot</label>
                  <input 
                    type="text" 
                    className="input-field" 
                    required 
                    placeholder="e.g. 10:00 AM - 11:30 AM" 
                    value={time} 
                    onChange={(e) => setTime(e.target.value)} 
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Room / Hall</label>
                  <input 
                    type="text" 
                    className="input-field" 
                    placeholder="e.g. Hall C1 / Lab 2" 
                    value={room} 
                    onChange={(e) => setRoom(e.target.value)} 
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Instructor</label>
                <input 
                  type="text" 
                  className="input-field" 
                  placeholder="e.g. Dr. Marvin Minsky" 
                  value={instructor} 
                  onChange={(e) => setInstructor(e.target.value)} 
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '8px' }}>Active Class Days</label>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {DAYS_OF_WEEK.map(day => (
                    <button
                      key={day}
                      type="button"
                      className={`btn btn-sm ${selectedDays.includes(day) ? 'btn-primary' : 'btn-secondary'}`}
                      onClick={() => toggleDaySelection(day)}
                    >
                      {day}
                    </button>
                  ))}
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '12px' }}>
                <button type="button" className="btn btn-secondary" style={{ width: '50%' }} onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ width: '50%' }}>
                  Save to Timetable
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
