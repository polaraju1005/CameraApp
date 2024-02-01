# import cv2
# import os
# import numpy as np
#
# def process_and_enhance_image(input_image_path,output_path):
#     image = cv2.imread(input_image_path)
#     if image is None:
#         print(f"Error: Could not load image from {input_image_path}")
#         return
#     image = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
#     thresh = cv2.adaptiveThreshold(image, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 21, 15)
#     dilate_kernel = np.ones((1, 1), np.uint8)
#     dilated = cv2.dilate(thresh, dilate_kernel, iterations=1)
#     # cv2.imwrite('final_image3.jpg', thresh, [int(cv2.IMWRITE_JPEG_QUALITY), 85])
#     #cv2.IMWRITE_PNG_COMPRESSION
#     if not os.path.exists(output_path):
#         os.makedirs(output_path)
#     output_path = os.path.join(output_path, "enhanced_image.jpg")
#     #cv2.imwrite(output_path, thresh, [int(cv2.IMWRITE_JPEG_QUALITY), 9])
#     cv2.imwrite(output_path, thresh)
#
#     return dilated
####################################################################################################

# import cv2
# import os
# import numpy as np
#
# def process_and_enhance_image(input_image_path,output_path):
#     image = cv2.imread(input_image_path)
#     if image is None:
#         print(f"Error: C3ould not load image from {input_image_path}")
#         return
#     image = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
#     thresh = cv2.adaptiveThreshold(image, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 21, 15)
#     dilate_kernel = np.ones((1, 1), np.uint8)
#     dilated = cv2.dilate(thresh, dilate_kernel, iterations=1)
#     # cv2.imwrite('final_image3.jpg', thresh, [int(cv2.IMWRITE_JPEG_QUALITY), 85])
#     #cv2.IMWRITE_PNG_COMPRESSION
#     if not os.path.exists(output_path):
#         os.makedirs(output_path)
#     output_path = os.path.join(output_path, "enhanced_image.jpg")
#     #cv2.imwrite(output_path, thresh, [int(cv2.IMWRITE_JPEG_QUALITY), 9])
#     cv2.imwrite(output_path, dilated)
#
#     return dilated

#####################################################################Prased ce shadow remove isnt working
# import cv2
# import os
# import numpy as np
#
# def process_and_enhance_image(input_image_path, output_path):
#     image = cv2.imread(input_image_path)
#     if image is None:
#         print(f"Error: Could not load image from {input_image_path}")
#         return
#
#
#     lab_image = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
#     l_channel, a_channel, b_channel = cv2.split(lab_image)
#     l_channel_eq = cv2.equalizeHist(l_channel)
#     enhanced_lab_image = cv2.merge((l_channel_eq, a_channel, b_channel))
#     enhanced_image = cv2.cvtColor(enhanced_lab_image, cv2.COLOR_LAB2BGR)
#     enhanced_image = cv2.bilateralFilter(enhanced_image, d=9, sigmaColor=75, sigmaSpace=75)
#     # kernel = np.array([[-1, -1, -1],
#     #                    [-1, 9, -1],
#     #                    [-1, -1, -1]], dtype=np.float32)
#     # sharpened_image = cv2.filter2D(enhanced_image, -1, kernel)
#
#     alpha= 2.0
#     beta= 25
#     sharpened_image = cv2.convertScaleAbs(enhanced_image, alpha=alpha, beta=beta)
#     if not os.path.exists(output_path):
#         os.makedirs(output_path)
#     output_path = os.path.join(output_path, "enhanced_image.jpg")
#     cv2.imwrite(output_path, sharpened_image)
#
#     return sharpened_image

######################################################################rachana working fine --some bg disturbances

import cv2
import numpy as np
import os

def process_and_enhance_image(image_path,output_path):
    # Load the image
    image = cv2.imread(image_path, -1)

    # Shadow Removal
    rgb_planes = cv2.split(image)
    result_planes = []
    result_norm_planes = []
    for plane in rgb_planes:
        dilated_img = cv2.dilate(plane, np.ones((7, 7), np.uint8))
        bg_img = cv2.medianBlur(dilated_img, 21)
        diff_img = 255 - cv2.absdiff(plane, bg_img)
        norm_img = cv2.normalize(diff_img, None, alpha=10, beta=250, norm_type=cv2.NORM_MINMAX, dtype=cv2.CV_8UC1)
        result_planes.append(diff_img)
        result_norm_planes.append(norm_img)

    shadow_removal_image = cv2.merge(result_planes)
    shadow_removal_image_norm = cv2.merge(result_norm_planes)

    # Color Enhancement
    lab = cv2.cvtColor(shadow_removal_image_norm, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)
    clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
    cl = clahe.apply(l)
    limg = cv2.merge((cl, a, b))
    enhanced_img = cv2.cvtColor(limg, cv2.COLOR_LAB2BGR)
    brightness = 30
    contrast = 60
    dummy = np.int16(enhanced_img)
    dummy = dummy * (contrast/127+1) - contrast + brightness
    dummy = np.clip(dummy, 0, 255)
    enhanced_img = np.uint8(dummy)
    hsv = cv2.cvtColor(enhanced_img, cv2.COLOR_BGR2HSV)
    h, s, v = cv2.split(hsv)
    s = cv2.add(s, 20)
    enhanced_img = cv2.merge([h, s, v])
    enhanced_img = cv2.cvtColor(enhanced_img, cv2.COLOR_HSV2BGR)

    if not os.path.exists(output_path):
        os.makedirs(output_path)

    output_path=os.path.join(output_path,"enhanced_image.jpg")


    cv2.imwrite(output_path,enhanced_img)
    return enhanced_img
############################################################ rachana color enhancement
# import cv2 as cv
# import numpy as np
# import os
# def map(x, in_min, in_max, out_min, out_max):
#     return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
# img = None;
# whitePoint = None;
# blackPoint = None;
# def highPassFilter(kSize):
#     global img
#     if not kSize % 2:
#         kSize += 1
#     kernel = np.ones((kSize, kSize), np.float32) / (kSize * kSize)
#     filtered = cv.filter2D(img, -1, kernel)
#     filtered = img.astype('float32') - filtered.astype('float32')
#     filtered = filtered + 127 * np.ones(img.shape, np.uint8)
#     filtered = filtered.astype('uint8')
#     img = filtered
# def process_and_enhance_image(image_path,output_dir):
#     img = cv.imread(image_path)
#     blackPoint = 66
#     whitePoint = 160
#     #blackPointSelect()
#     img = img.astype('int32')
#     img = map(img, blackPoint, 255, 0, 255)
#     _, img = cv.threshold(img, 0, 255, cv.THRESH_TOZERO)
#     img = img.astype('uint8')
#     #whitePointSelect()
#     _, img = cv.threshold(img, whitePoint, 255, cv.THRESH_TRUNC)
#     img = img.astype('int32')
#     img = map(img, 0, whitePoint, 0, 255)
#     img = img.astype('uint8')
#     if not os.path.exists(output_dir):
#         os.makedirs(output_dir)
#     output_dir=os.path.join(output_dir,"enhanced_image.jpg")
#     cv.imwrite(output_dir, img)
#     return img
#
# # # process_and_enhanced_image('Inputs/img_4.png')

