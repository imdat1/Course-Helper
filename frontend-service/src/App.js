// App.js
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './utils/AuthContext';
import Navbar from './components/common/Navbar';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import CourseList from './components/courses/CourseList';
import CourseForm from './components/courses/CourseForm';
import CourseDetail from './components/courses/CourseDetail';
import ChatInterface from './components/chat/ChatInterface';
import FlashCardsList from './components/courses/FlashCardsList';
import QuizView from './components/courses/QuizView';
import PrivateRoute from './components/common/PrivateRoute';
import 'bootstrap/dist/css/bootstrap.min.css';
import './styles.css';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Navbar />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          <Route path="/courses" element={
            <PrivateRoute>
              <CourseList />
            </PrivateRoute>
          } />
          
          <Route path="/courses/new" element={
            <PrivateRoute>
              <CourseForm />
            </PrivateRoute>
          } />
          
          <Route path="/courses/:id" element={
            <PrivateRoute>
              <CourseDetail />
            </PrivateRoute>
          } />
          
          <Route path="/courses/:id/edit" element={
            <PrivateRoute>
              <CourseForm />
            </PrivateRoute>
          } />
          
          <Route path="/courses/:id/chat" element={
            <PrivateRoute>
              <ChatInterface />
            </PrivateRoute>
          } />
          
          {/* Add this new route */}
          <Route path="/courses/:id/flashcards" element={
            <PrivateRoute>
              <FlashCardsList />
            </PrivateRoute>
          } />

          <Route path="/courses/:id/quizzes/:fileId" element={
            <PrivateRoute>
              <QuizView />
            </PrivateRoute>
          } />
          
          <Route path="/" element={<Navigate to="/courses" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;