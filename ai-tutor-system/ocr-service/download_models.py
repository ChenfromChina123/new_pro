#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
模型下载脚本 - 下载CRNN ONNX量化模型
支持从多个源下载轻量级OCR模型
"""

import os
import urllib.request
import hashlib
from pathlib import Path

# 模型目录
MODELS_DIR = Path(__file__).parent / "models"
MODELS_DIR.mkdir(exist_ok=True)

# 轻量级OCR模型配置
# 使用PP-OCRv4超轻量模型的ONNX版本
MODELS = {
    # 文本检测模型 - 超轻量版 (~4MB)
    "det_model": {
        "url": "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_det_infer.tar",
        "filename": "ch_PP-OCRv4_det_infer.tar",
        "onnx_url": "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_det_infer.onnx",
        "onnx_filename": "det_model.onnx",
        "size_mb": 4.5
    },
    # 文本识别模型 - 超轻量版 (~10MB)
    "rec_model": {
        "url": "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_rec_infer.tar",
        "filename": "ch_PP-OCRv4_rec_infer.tar",
        "onnx_url": "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_rec_infer.onnx",
        "onnx_filename": "rec_model.onnx",
        "size_mb": 10.0
    },
    # 文本方向分类模型 (~2MB)
    "cls_model": {
        "url": "https://paddleocr.bj.bcebos.com/dygraph_v2.0/ch/ch_ppocr_mobile_v2.0_cls_infer.tar",
        "filename": "ch_ppocr_mobile_v2.0_cls_infer.tar",
        "onnx_url": "https://paddleocr.bj.bcebos.com/dygraph_v2.0/ch/ch_ppocr_mobile_v2.0_cls_infer.onnx",
        "onnx_filename": "cls_model.onnx",
        "size_mb": 2.2
    }
}

# 字典文件 - 用于识别结果解码
DICT_URL = "https://paddleocr.bj.bcebos.com/ppocr_keys_v1.txt"
DICT_FILENAME = "ppocr_keys_v1.txt"


def download_file(url: str, filepath: Path, desc: str = None):
    """
    下载文件并显示进度
    """
    if desc:
        print(f"正在下载: {desc}")
    print(f"URL: {url}")
    print(f"保存到: {filepath}")
    
    def progress_hook(count, block_size, total_size):
        percent = int(count * block_size * 100 / total_size)
        percent = min(percent, 100)
        mb_downloaded = count * block_size / (1024 * 1024)
        mb_total = total_size / (1024 * 1024)
        print(f"\r进度: {percent}% ({mb_downloaded:.1f}MB / {mb_total:.1f}MB)", end="", flush=True)
    
    try:
        urllib.request.urlretrieve(url, filepath, progress_hook)
        print("\n下载完成!")
        return True
    except Exception as e:
        print(f"\n下载失败: {e}")
        return False


def download_all_models():
    """
    下载所有模型文件
    """
    print("=" * 60)
    print("开始下载OCR模型文件")
    print("=" * 60)
    
    success_count = 0
    total_count = len(MODELS) + 1  # +1 for dict file
    
    # 下载字典文件
    dict_path = MODELS_DIR / DICT_FILENAME
    if not dict_path.exists():
        if download_file(DICT_URL, dict_path, "字典文件"):
            success_count += 1
    else:
        print(f"字典文件已存在: {dict_path}")
        success_count += 1
    
    # 下载模型文件
    for model_name, model_info in MODELS.items():
        print(f"\n{'=' * 40}")
        
        # 优先下载ONNX格式
        onnx_path = MODELS_DIR / model_info["onnx_filename"]
        if not onnx_path.exists():
            if download_file(model_info["onnx_url"], onnx_path, f"{model_name} ONNX模型"):
                success_count += 1
        else:
            print(f"模型已存在: {onnx_path}")
            success_count += 1
    
    print("\n" + "=" * 60)
    print(f"下载完成! 成功: {success_count}/{total_count}")
    print(f"模型目录: {MODELS_DIR}")
    
    # 显示模型大小
    total_size = 0
    for f in MODELS_DIR.iterdir():
        size_mb = f.stat().st_size / (1024 * 1024)
        total_size += size_mb
        print(f"  - {f.name}: {size_mb:.2f} MB")
    print(f"总大小: {total_size:.2f} MB")
    
    return success_count == total_count


def create_char_dict():
    """
    创建字符字典
    """
    dict_path = MODELS_DIR / DICT_FILENAME
    
    if dict_path.exists():
        with open(dict_path, 'r', encoding='utf-8') as f:
            chars = [line.strip() for line in f.readlines()]
        chars = ['blank'] + chars  # 添加blank token
        print(f"字典加载成功, 共 {len(chars)} 个字符")
        return chars
    else:
        print("字典文件不存在，请先运行下载")
        return None


if __name__ == "__main__":
    download_all_models()
