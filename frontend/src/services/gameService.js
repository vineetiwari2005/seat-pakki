import api from './api';

// Game Service - all game-related API calls
const gameService = {
  /**
   * Submit game score and receive reward
   * @param {number} userId
   * @param {number} score
   * @param {number} levelReached
   * @param {number} gridSizeReached
   * @returns {Promise}
   */
  submitGameScore: async (userId, score, levelReached, gridSizeReached) => {
    try {
      const response = await api.post('/api/game/submit-score', {
        userId,
        score,
        levelReached,
        gridSizeReached,
      });
      return response.data;
    } catch (error) {
      console.error('Error submitting game score:', error);
      throw error.response?.data || error;
    }
  },

  /**
   * Get user's active rewards from temporary wallet
   * @param {number} userId
   * @returns {Promise}
   */
  getUserActiveRewards: async (userId) => {
    try {
      const response = await api.get(`/api/game/user/${userId}/rewards`);
      return response.data;
    } catch (error) {
      console.error('Error fetching user rewards:', error);
      throw error.response?.data || error;
    }
  },

  /**
   * Get total amount of active rewards for user
   * @param {number} userId
   * @returns {Promise}
   */
  getUserTotalRewardAmount: async (userId) => {
    try {
      const response = await api.get(`/api/game/user/${userId}/total-rewards`);
      return response.data;
    } catch (error) {
      console.error('Error fetching total reward amount:', error);
      throw error.response?.data || error;
    }
  },

  /**
   * Mark expired rewards across all users
   * @returns {Promise}
   */
  markExpiredRewards: async () => {
    try {
      const response = await api.post('/api/game/mark-expired');
      return response.data;
    } catch (error) {
      console.error('Error marking expired rewards:', error);
      throw error.response?.data || error;
    }
  },

  /**
   * Use a specific reward
   * @param {number} rewardId
   * @returns {Promise}
   */
  useReward: async (rewardId) => {
    try {
      const response = await api.post(`/api/game/use-reward/${rewardId}`);
      return response.data;
    } catch (error) {
      console.error('Error using reward:', error);
      throw error.response?.data || error;
    }
  },
};

export default gameService;
