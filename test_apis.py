#!/usr/bin/env python3
"""
Test script for Gradia Backend Candidate APIs
Run this after starting the Spring Boot server
"""

import requests
import json
import sys

BASE_URL = "http://localhost:8081/api/candidates"

def print_section(title):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}\n")

def print_success(message):
    print(f"✅ {message}")

def print_error(message):
    print(f"❌ {message}")

def print_info(message):
    print(f"ℹ️  {message}")

def test_registration():
    print_section("Test 1: Candidate Registration")
    
    payload = {
        "email": "test.candidate@example.com",
        "password": "password123"
    }
    
    print(f"POST {BASE_URL}/register")
    print(f"Payload: {json.dumps(payload, indent=2)}")
    
    try:
        response = requests.post(f"{BASE_URL}/register", json=payload)
        print(f"\nStatus Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print_success("Registration successful!")
            print(f"\nResponse:\n{json.dumps(data, indent=2)}")
            
            # Extract token
            if data.get('success') and data.get('data', {}).get('token'):
                token = data['data']['token']
                print_info(f"Token received: {token[:50]}...")
                return token, data['data']['user']
            else:
                print_error("No token in response")
                return None, None
        else:
            print_error(f"Registration failed (HTTP {response.status_code})")
            print(f"Response: {response.text}")
            return None, None
            
    except requests.exceptions.ConnectionError:
        print_error("Cannot connect to server. Is the Spring Boot server running on http://localhost:8081?")
        return None, None
    except Exception as e:
        print_error(f"Error: {str(e)}")
        return None, None

def test_login():
    print_section("Test 2: Candidate Login")
    
    payload = {
        "email": "test.candidate@example.com",
        "password": "password123"
    }
    
    print(f"POST {BASE_URL}/login")
    print(f"Payload: {json.dumps(payload, indent=2)}")
    
    try:
        response = requests.post(f"{BASE_URL}/login", json=payload)
        print(f"\nStatus Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print_success("Login successful!")
            print(f"\nResponse:\n{json.dumps(data, indent=2)}")
            
            # Extract token
            if data.get('success') and data.get('data', {}).get('token'):
                token = data['data']['token']
                user_info = data['data']['user']
                print_info(f"Token received: {token[:50]}...")
                print_info(f"Profile complete: {user_info.get('profileComplete', False)}")
                return token
            else:
                print_error("No token in response")
                return None
        else:
            print_error(f"Login failed (HTTP {response.status_code})")
            print(f"Response: {response.text}")
            return None
            
    except requests.exceptions.ConnectionError:
        print_error("Cannot connect to server. Is the Spring Boot server running?")
        return None
    except Exception as e:
        print_error(f"Error: {str(e)}")
        return None

def test_create_profile(token):
    print_section("Test 3: Create/Update Profile")
    
    if not token:
        print_error("No authentication token available. Skipping profile test.")
        return False
    
    payload = {
        "fullName": "John Doe",
        "mobile": "+1234567890",
        "location": "New York, USA",
        "linkedin": "https://linkedin.com/in/johndoe",
        "profilePicture": "https://example.com/picture.jpg",
        "resumeUrl": "https://example.com/resume.pdf",
        "experienceLevel": "Mid-level",
        "preferredRole": "Software Engineer"
    }
    
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    
    print(f"POST {BASE_URL}/profile")
    print(f"Headers: Authorization: Bearer {token[:30]}...")
    print(f"Payload: {json.dumps(payload, indent=2)}")
    
    try:
        response = requests.post(f"{BASE_URL}/profile", json=payload, headers=headers)
        print(f"\nStatus Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print_success("Profile created successfully!")
            print(f"\nResponse:\n{json.dumps(data, indent=2)}")
            return True
        else:
            print_error(f"Profile creation failed (HTTP {response.status_code})")
            print(f"Response: {response.text}")
            return False
            
    except Exception as e:
        print_error(f"Error: {str(e)}")
        return False

def test_get_profile(token):
    print_section("Test 4: Get Profile")
    
    if not token:
        print_error("No authentication token available. Skipping get profile test.")
        return False
    
    headers = {
        "Authorization": f"Bearer {token}"
    }
    
    print(f"GET {BASE_URL}/profile")
    print(f"Headers: Authorization: Bearer {token[:30]}...")
    
    try:
        response = requests.get(f"{BASE_URL}/profile", headers=headers)
        print(f"\nStatus Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print_success("Profile retrieved successfully!")
            print(f"\nResponse:\n{json.dumps(data, indent=2)}")
            return True
        else:
            print_error(f"Get profile failed (HTTP {response.status_code})")
            print(f"Response: {response.text}")
            return False
            
    except Exception as e:
        print_error(f"Error: {str(e)}")
        return False

def test_invalid_login():
    print_section("Test 5: Invalid Login (Negative Test)")
    
    payload = {
        "email": "test.candidate@example.com",
        "password": "wrongpassword"
    }
    
    print(f"POST {BASE_URL}/login")
    print(f"Payload: {json.dumps(payload, indent=2)}")
    
    try:
        response = requests.post(f"{BASE_URL}/login", json=payload)
        print(f"\nStatus Code: {response.status_code}")
        
        if response.status_code in [401, 400]:
            print_success(f"Invalid login correctly rejected (HTTP {response.status_code})")
            print(f"Response: {response.text}")
            return True
        else:
            print_error(f"Unexpected response (HTTP {response.status_code})")
            print(f"Response: {response.text}")
            return False
            
    except Exception as e:
        print_error(f"Error: {str(e)}")
        return False

def main():
    print("\n" + "="*60)
    print("  Gradia Backend API Testing")
    print("="*60)
    
    # Check if server is running
    try:
        response = requests.get("http://localhost:8081/actuator/health", timeout=2)
    except:
        try:
            # Try a simple request to see if server is up
            response = requests.get("http://localhost:8081/api/candidates/login", timeout=2)
        except requests.exceptions.ConnectionError:
            print_error("\nCannot connect to server at http://localhost:8081")
            print_info("Please make sure:")
            print_info("  1. PostgreSQL is installed and running")
            print_info("  2. Database 'gradia_db' is created")
            print_info("  3. Spring Boot server is running (mvn spring-boot:run)")
            sys.exit(1)
    
    # Run tests
    token, user = test_registration()
    
    # If registration didn't work, try login (user might already exist)
    if not token:
        token = test_login()
    
    if token:
        test_create_profile(token)
        test_get_profile(token)
    else:
        print_error("Cannot proceed with profile tests without authentication token")
    
    test_invalid_login()
    
    print_section("Testing Complete")
    print_success("All tests executed!")

if __name__ == "__main__":
    main()

