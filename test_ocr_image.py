import requests
import base64

# 测试OCR功能（使用真实图片）
def test_ocr_with_image():
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
    
    # 读取测试图片并转换为base64
    image_path = r"d:\Users\Administrator\AistudyProject\new_pro\ai-tutor-system\ocr-service\temp_test.png"
    with open(image_path, "rb") as f:
        image_bytes = f.read()
    image_base64 = base64.b64encode(image_bytes).decode("utf-8")
    
    # 测试OCR识别（Base64方式）
    ocr_url = "http://localhost:5000/api/ocr/recognize/base64"
    ocr_data = {
        "image": image_base64
    }
    
    print("正在识别图片...")
    ocr_response = requests.post(ocr_url, json=ocr_data, headers=headers)
    print(f"Status code: {ocr_response.status_code}")
    print(f"Response: {ocr_response.json()}")
    
    # 测试OCR识别（文件上传方式）
    ocr_url2 = "http://localhost:5000/api/ocr/recognize"
    files = {
        "image": ("test.png", open(image_path, "rb"), "image/png")
    }
    
    print("\n正在识别图片（文件上传方式）...")
    ocr_response2 = requests.post(ocr_url2, files=files, headers=headers)
    print(f"Status code: {ocr_response2.status_code}")
    print(f"Response: {ocr_response2.json()}")
    
    print("\nTest completed!")

if __name__ == "__main__":
    test_ocr_with_image()
