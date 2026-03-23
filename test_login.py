import requests

# 测试登录获取token
def test_login():
    url = "http://localhost:5000/api/auth/login"
    data = {
        "email": "3301767269@qq.com",
        "password": "123456"
    }
    
    response = requests.post(url, json=data)
    print("Login response:")
    print(f"Status code: {response.status_code}")
    print(f"Response: {response.json()}")
    
    if response.status_code == 200:
        token = response.json().get("data").get("access_token")
        print(f"\nToken: {token}")
        return token
    else:
        print("Login failed")
        return None

if __name__ == "__main__":
    test_login()