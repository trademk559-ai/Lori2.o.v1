import dotenv from 'dotenv';
dotenv.config();

export const config = {
  port: parseInt(process.env.PORT || '3000', 10),
  nodeEnv: process.env.NODE_ENV || 'development',
  jwtSecret: process.env.JWT_SECRET || 'lori_super_secure_private_jwt_key_9921',
  jwtRefreshSecret: process.env.JWT_REFRESH_SECRET || 'lori_super_secure_refresh_key_9922',
  accessTokenExpiry: '2h',
  refreshTokenExpiry: '30d',
  geminiApiKey: process.env.GEMINI_API_KEY || '',
  geminiModel: process.env.GEMINI_MODEL || 'gemini-2.5-flash',
  database: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432', 10),
    user: process.env.DB_USER || 'lori_admin',
    password: process.env.DB_PASSWORD || 'lori_secure_pass',
    database: process.env.DB_NAME || 'lori_assistant_db',
  },
  corsOrigin: process.env.CORS_ORIGIN || '*'
};
