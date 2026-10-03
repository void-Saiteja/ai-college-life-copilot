import { config } from '../config/env.js';

// Smart rule-based / LLM fallback generator
export const summarizeNotes = async (req, res, next) => {
  try {
    const { content, title, course } = req.body;
    if (!content) {
      return res.status(400).json({ success: false, error: 'Content is required for AI summarization' });
    }

    // Split sentences for key takeaways extraction
    const sentences = content.split(/(?<=[.?!])\s+/).filter(Boolean);
    const words = content.split(/\s+/).length;

    const keyTakeaways = sentences.slice(0, Math.min(4, sentences.length)).map((s, idx) => `• ${s.trim()}`);
    
    // Auto-generate flashcards
    const flashcards = [
      {
        question: `What is the core definition presented in "${title || 'this topic'}"?`,
        answer: sentences[0] || 'Core foundational concept from the study notes.'
      },
      {
        question: `Why is this important for ${course || 'the subject'}?`,
        answer: sentences[1] || 'It provides the underlying principles needed to solve advanced problems.'
      },
      {
        question: `What is a key application or operation mentioned?`,
        answer: sentences[2] || 'Applying structured methods to streamline computation and accuracy.'
      }
    ];

    // Auto-generate practice quiz
    const quiz = [
      {
        id: 1,
        question: `Which key concept is most central to ${title || 'these notes'}?`,
        options: [
          sentences[0]?.substring(0, 50) || 'Primary principle',
          'Random unrelated formula',
          'Deprecated legacy method',
          'External environmental factor'
        ],
        correctAnswer: 0,
        explanation: 'Directly stated in the primary summary definition.'
      },
      {
        id: 2,
        question: `What is the main benefit or requirement highlighted?`,
        options: [
          'High memory overhead',
          sentences[1]?.substring(0, 50) || 'Optimal efficiency and structure',
          'Manual error check requirement',
          'None of the above'
        ],
        correctAnswer: 1,
        explanation: 'Extracted from key takeaway notes.'
      }
    ];

    const response = {
      title: title || 'AI Generated Note Summary',
      course: course || 'General Study',
      wordCount: words,
      readingTimeMinutes: Math.ceil(words / 180),
      summary: `This note covers essential topics for ${course || 'your course'}, focusing on ${sentences.slice(0, 2).join(' ')}`,
      keyTakeaways,
      flashcards,
      quiz
    };

    return res.json({ success: true, data: response });
  } catch (error) {
    next(error);
  }
};

export const prioritizeTasks = async (req, res, next) => {
  try {
    const { assignments } = req.body;
    if (!assignments || !Array.isArray(assignments)) {
      return res.status(400).json({ success: false, error: 'Assignments array is required' });
    }

    const today = new Date();
    
    const analyzed = assignments.map(item => {
      const due = new Date(item.dueDate);
      const diffDays = Math.ceil((due - today) / (1000 * 60 * 60 * 24));
      
      let urgencyScore = 0;
      if (diffDays <= 1) urgencyScore = 95;
      else if (diffDays <= 3) urgencyScore = 80;
      else if (diffDays <= 7) urgencyScore = 60;
      else urgencyScore = 30;

      const priorityWeight = item.priority === 'High' ? 25 : item.priority === 'Medium' ? 15 : 5;
      const hoursWeight = (item.estimatedHours || 3) * 5;
      const totalScore = Math.min(100, urgencyScore + priorityWeight + hoursWeight);

      let quadrant = 'Do First';
      if (totalScore > 80) quadrant = 'Do First (Urgent & Important)';
      else if (totalScore > 60) quadrant = 'Schedule (Important, Less Urgent)';
      else if (totalScore > 40) quadrant = 'Delegate / Quick Win';
      else quadrant = 'Low Priority / Save for Weekend';

      return {
        ...item,
        score: totalScore,
        daysLeft: diffDays,
        quadrant,
        suggestedPomodoros: Math.ceil((item.estimatedHours || 2) * 2),
        aiAdvice: diffDays <= 2 
          ? `🔥 Critical deadline approaching in ${diffDays <= 0 ? 'TODAY' : diffDays + ' day(s)'}! Break work into 45-min hyper-focus blocks.`
          : `✅ On track. Dedicate 1.5 hours tomorrow evening to keep momentum.`
      };
    }).sort((a, b) => b.score - a.score);

    // Calculate overall workload stress level
    const totalHours = analyzed.reduce((acc, curr) => acc + (curr.estimatedHours || 0), 0);
    let stressLevel = 'Low';
    if (totalHours > 15) stressLevel = 'High (Workload Overload Alert)';
    else if (totalHours > 8) stressLevel = 'Moderate (Manageable)';

    return res.json({
      success: true,
      data: {
        totalPendingHours: totalHours,
        stressLevel,
        prioritizedTasks: analyzed,
        recommendation: totalHours > 12 
          ? '⚠️ High workload detected! We recommend prioritizing top 2 urgent items today and rescheduling non-essential tasks.'
          : '🌟 Workload is well balanced! You can tackle high-value tasks with clear focus.'
      }
    });
  } catch (error) {
    next(error);
  }
};

export const chatCopilot = async (req, res, next) => {
  try {
    const { message, context } = req.body;
    if (!message) {
      return res.status(400).json({ success: false, error: 'Message query is required' });
    }

    const lower = message.toLowerCase();
    let reply = '';

    if (lower.includes('exam') || lower.includes('study') || lower.includes('test')) {
      reply = `📚 **AI Study Tip**: For upcoming exams, practice active recall! Instead of passively reading notes, test yourself using flashcards or explain the concept out loud. Also, apply the **Pomodoro Technique**: 25 min study + 5 min break.`;
    } else if (lower.includes('stress') || lower.includes('tired') || lower.includes('burnout')) {
      reply = `🧘 **Copilot Wellness Check**: Take a deep breath! College can get intense. Remember to drink water, get at least 7 hours of sleep, and break large tasks into 20-minute chunks. You've got this!`;
    } else if (lower.includes('budget') || lower.includes('money') || lower.includes('save') || lower.includes('cost')) {
      reply = `💰 **Smart College Finance Advice**: Cook in batches on Sundays, look for student discounts (UNiDAYS, Spotify Student, GitHub Student Pack), and stick to your daily spending target.`;
    } else if (lower.includes('schedule') || lower.includes('time') || lower.includes('planner')) {
      reply = `📅 **Time Management Strategy**: Block out specific time slots for studying, classes, and relaxation. Protect your high-energy hours (morning or evening) for hard technical assignments.`;
    } else {
      reply = `🤖 **AI College Copilot**: I'm here to support your student life! I can help you organize assignments, generate quizzes from notes, analyze your budget, or build a personalized study schedule. How can I assist you right now?`;
    }

    return res.json({
      success: true,
      reply,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    next(error);
  }
};

export const mealAndBudgetPlanner = async (req, res, next) => {
  try {
    const { budgetRemaining, ingredients = [] } = req.body;
    
    const mealIdeas = [
      {
        name: 'Quick College Ramen Upgrade',
        cost: '$2.50',
        time: '10 mins',
        ingredients: ['Instant Ramen', 'Egg', 'Green Onions', 'Spinach'],
        instructions: 'Boil ramen, crack egg into broth during last 2 mins, stir in fresh spinach.'
      },
      {
        name: 'Loaded High-Protein Burrito Bowl',
        cost: '$4.20',
        time: '15 mins',
        ingredients: ['Rice', 'Black Beans', 'Canned Chicken/Tofu', 'Salsa', 'Cheese'],
        instructions: 'Heat rice and beans, top with salsa, cheese, and protein choice.'
      },
      {
        name: '5-Minute Peanut Butter Banana Oats',
        cost: '$1.80',
        time: '5 mins',
        ingredients: ['Rolled Oats', 'Milk/Water', 'Peanut Butter', 'Banana', 'Honey'],
        instructions: 'Microwave oats with milk for 90s, swirl in peanut butter, top with sliced banana.'
      }
    ];

    return res.json({
      success: true,
      data: {
        budgetAdvice: budgetRemaining < 100 
          ? '⚠️ Budget tight this month! Stick to meal prep and campus dining events.'
          : '👍 Healthy budget margin! Remember to put aside 15% into savings.',
        suggestedMeals: mealIdeas
      }
    });
  } catch (error) {
    next(error);
  }
};
