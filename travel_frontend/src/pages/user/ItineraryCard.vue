<template>
  <div class="itinerary-card">
    <div class="card-header">
      <h3>🗺️ {{ itinerary.destination }}{{ itinerary.days }}日游</h3>
      <div class="header-actions">
        <span v-if="itinerary.totalEstimatedCost" class="total-cost">
          💰 预算: ¥{{ itinerary.totalEstimatedCost }}
        </span>
        <button class="save-btn" @click="$emit('save', itinerary)">
          💾 保存行程
        </button>
      </div>
    </div>
    
    <div v-if="itinerary.theme" class="theme-tag">
      🏷️ {{ itinerary.theme }}
    </div>
    
    <div class="daily-plans">
      <div v-for="plan in itinerary.dailyPlans" :key="plan.day" class="daily-plan">
        <div class="day-header">
          <span class="day-badge">第{{ plan.day }}天</span>
          <span v-if="plan.date" class="day-date">{{ plan.date }}</span>
        </div>
        
        <div class="activities">
          <div v-for="(activity, idx) in plan.activities" :key="idx" class="activity-item">
            <div class="activity-time">{{ activity.time }}</div>
            <div class="activity-content">
              <h4>{{ getActivityIcon(activity.type) }} {{ activity.name }}</h4>
              <p>{{ activity.description }}</p>
              
              <div v-if="activity.location" class="location">
                📍 {{ activity.location.address }}
              </div>
              
              <div v-if="activity.estimatedCost" class="cost">
                💰 约 ¥{{ activity.estimatedCost }}
              </div>
              
              <div v-if="activity.tips && activity.tips.length" class="tips">
                💡 {{ activity.tips.join(' · ') }}
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="plan.meals && plan.meals.length" class="meals">
          <h4 class="meals-title">🍽️ 用餐推荐</h4>
          <div v-for="(meal, idx) in plan.meals" :key="idx" class="meal-item">
            <span class="meal-time">{{ meal.time }}</span>
            <span class="meal-type">{{ getMealType(meal.type) }}</span>
            <span class="meal-rec">{{ meal.recommendation }}</span>
            <span v-if="meal.estimatedCost" class="meal-cost">¥{{ meal.estimatedCost }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="itinerary.tips && itinerary.tips.length" class="itinerary-tips">
      <h4>💡 温馨提示</h4>
      <ul>
        <li v-for="(tip, idx) in itinerary.tips" :key="idx">{{ tip }}</li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { StructuredItinerary, Activity } from '@/types/itinerary'

interface Props {
  itinerary: StructuredItinerary
}

defineProps<Props>()
defineEmits<{
  (e: 'save', itinerary: StructuredItinerary): void
}>()

function getActivityIcon(type: Activity['type']): string {
  const icons = {
    attraction: '🏞️',
    transport: '🚗',
    rest: '☕',
    meal: '🍽️'
  }
  return icons[type] || '📍'
}

function getMealType(type: 'breakfast' | 'lunch' | 'dinner'): string {
  const types = {
    breakfast: '早餐',
    lunch: '午餐',
    dinner: '晚餐'
  }
  return types[type] || type
}
</script>

<style scoped>
.itinerary-card {
  background: var(--gradient-ocean);
  border-radius: 16px;
  padding: 24px;
  margin: 16px 0;
  color: white;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.card-header h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total-cost {
  background: rgba(255, 255, 255, 0.2);
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
}

.save-btn {
  background: var(--color-bg-secondary);
  color: var(--primary-500);
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
  font-size: 14px;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.theme-tag {
  background: rgba(255, 255, 255, 0.15);
  padding: 8px 16px;
  border-radius: 20px;
  display: inline-block;
  margin-bottom: 20px;
  font-size: 14px;
}

.daily-plans {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.daily-plan {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(10px);
}

.day-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.day-badge {
  background: rgba(255, 255, 255, 0.25);
  padding: 8px 20px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 16px;
}

.day-date {
  font-size: 14px;
  opacity: 0.9;
}

.activities {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  gap: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  transition: all 0.2s;
}

.activity-item:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(4px);
}

.activity-time {
  font-weight: 600;
  min-width: 100px;
  font-size: 14px;
  opacity: 0.9;
}

.activity-content {
  flex: 1;
}

.activity-content h4 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}

.activity-content p {
  margin: 0 0 8px 0;
  opacity: 0.9;
  font-size: 14px;
  line-height: 1.6;
}

.location, .cost, .tips {
  font-size: 13px;
  margin-top: 6px;
  opacity: 0.85;
  line-height: 1.5;
}

.meals {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.meals-title {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
}

.meal-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  margin-bottom: 8px;
  font-size: 14px;
}

.meal-time {
  font-weight: 600;
  min-width: 80px;
}

.meal-type {
  background: rgba(255, 255, 255, 0.15);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.meal-rec {
  flex: 1;
  opacity: 0.9;
}

.meal-cost {
  font-weight: 600;
  opacity: 0.9;
}

.itinerary-tips {
  margin-top: 20px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}

.itinerary-tips h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
}

.itinerary-tips ul {
  margin: 0;
  padding-left: 20px;
  list-style: none;
}

.itinerary-tips li {
  margin-bottom: 8px;
  font-size: 14px;
  opacity: 0.9;
  line-height: 1.6;
  position: relative;
}

.itinerary-tips li::before {
  content: '•';
  position: absolute;
  left: -15px;
  font-weight: bold;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .itinerary-card {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .activity-item {
    flex-direction: column;
    gap: 8px;
  }
  
  .activity-time {
    min-width: auto;
  }
  
  .meal-item {
    flex-wrap: wrap;
  }
}
</style>
