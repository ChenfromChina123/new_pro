import requests
import base64

# 测试OCR功能
def test_ocr():
    # 先获取token
    login_url = "http://localhost:5000/api/auth/login"
    login_data = {
        "email": "3301767269@qq.com",
        "password": "123456"
    }
    
    login_response = requests.post(login_url, json=login_data)
    if login_response.status_code != 200:
        print("Login failed")
        return
    
    token = login_response.json().get("data").get("access_token")
    headers = {
        "Authorization": f"Bearer {token}"
    }
    
    # 测试OCR健康检查
    health_url = "http://localhost:5000/api/ocr/health"
    health_response = requests.get(health_url, headers=headers)
    print("OCR Health Check:")
    print(f"Status code: {health_response.status_code}")
    print(f"Response: {health_response.json()}")
    print()
    
    # 测试OCR识别（Base64方式）
    # 使用一个简单的测试图片（这里使用一个空白图片的base64编码作为示例）
    # 实际测试时可以替换为真实图片的base64编码
    test_image_base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
    
    ocr_url = "http://localhost:5000/api/ocr/recognize/base64"
    ocr_data = {
        "image": test_image_base64
    }
    
    ocr_response = requests.post(ocr_url, json=ocr_data, headers=headers)
    print("OCR Recognize Base64:")
    print(f"Status code: {ocr_response.status_code}")
    print(f"Response: {ocr_response.json()}")
    print()
    
    print("Test completed!")

if __name__ == "__main__":
    test_ocr()