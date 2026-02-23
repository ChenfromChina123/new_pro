import requests
import json

def test_reset_password():
    url = "http://localhost:5000/api/auth/forgot-password"
    data = {
        "email": "1836078388@qq.com",
        "code": "324614",
        "newPassword": "newpassword123"
    }
    
    headers = {
        "Content-Type": "application/json"
    }
    
    print(f"发送重置密码请求: {data}")
    try:
        response = requests.post(url, json=data, headers=headers)
        print(f"状态码: {response.status_code}")
        print(f"响应内容: {response.text}")
        
        if response.status_code == 200:
            print("密码重置成功验证通过！")
        else:
            print("密码重置失败！")
    except Exception as e:
        print(f"请求发生异常: {e}")

if __name__ == "__main__":
    test_reset_password()
