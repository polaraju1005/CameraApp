#include <jni.h>
#include <random>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/ml.hpp>

#define TAG "CVLib"

using namespace std;
using namespace cv;

extern "C" {

  const int MIN_SIZE = 300;
  const int MARGIN_OUTER = 5;
  const int MARGIN_INNER = 50;

void JNICALL
Java_com_example_cameraxapp_CVLib_getReducedImage(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong resultAddr) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &result = *(Mat *) resultAddr;

  // Get image dimensions
  int w = mat.cols;
  int h = mat.rows;

  // Resize smaller side to 500 (arbitrary)
  if(h > w) {
    h = h * MIN_SIZE / w;
    w = MIN_SIZE;
  } else {
    w = w * MIN_SIZE / h;
    h = MIN_SIZE;
  }

  // Resize image
  resize(mat, result, Size(w, h));
}

void JNICALL
Java_com_example_cameraxapp_CVLib_getDocumentMask(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong maskAddr) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &mask = *(Mat *) maskAddr;

  // Get image dimensions
  int w = mat.cols;
  int h = mat.rows;

  // Create marker matrix
  Mat markers = Mat::zeros(h, w, CV_32S);

  // Mark region background = 1
  rectangle(markers, Point(0, 0), Point(MARGIN_OUTER, h), 1, -1);
  rectangle(markers, Point(0, 0), Point(w, MARGIN_OUTER), 1, -1);
  rectangle(markers, Point(0, h-MARGIN_OUTER), Point(w, h), 1, -1);
  rectangle(markers, Point(w-MARGIN_OUTER, 0), Point(w, h), 1, -1);

  // Mark region paper = 2
  rectangle(markers, 
      Point(w/2 - MARGIN_INNER, h/2 - MARGIN_INNER), 
      Point(w/2+50, h/2+50), 
      2, -1);

  // Do watershed
  Mat blur;
  GaussianBlur(mat, blur, Size(13,13), 0);
  watershed(blur, markers);

  // Document mask
  mask = Mat::zeros(h, w, CV_8U);

  for(int i=0;i < h; ++i) {
    for(int j=0;j < w; ++j) {
      int index = markers.at<int>(i, j);
      if(index == 2) {
        mask.at<uint8_t>(i,j) = 255;
      }
    }
  }
}

jboolean JNICALL
Java_com_example_cameraxapp_CVLib_getDocumentContour(JNIEnv *env,
    jobject instance,
    jlong maskAddr,
    jlong cntAddr) {

  // get Mat from raw address
  Mat &mask = *(Mat *) maskAddr;
  Mat &cnt = *(Mat *) cntAddr;

  // Get image dimensions
  int w = mask.cols;
  int h = mask.rows;

  // Find all contours
  vector<vector<Point> > cnts;
  findContours(mask, cnts, RETR_LIST, CHAIN_APPROX_SIMPLE);

  if(cnts.size() == 0) {
    return false;
  }

  // Find the biggest contour
  float best_area = 0.f;
  int best = -1;
  
  for(int i=0; i<(int)cnts.size(); ++i) {
    float area = contourArea(cnts[i]);
    if(area > best_area) {
      best_area = area;
      best = i;
    }
  }

  cnt = Mat(cnts[best], true);
  return true;
}

jboolean JNICALL
Java_com_example_cameraxapp_CVLib_getDocumentApproxContour(JNIEnv *env,
    jobject instance,
    jlong cntAddr,
    jlong cntApproxAddr
    ) {

  // get Mat from raw address
  Mat &cntMat = *(Mat *) cntAddr;
  Mat &cntApprox = *(Mat *) cntApproxAddr;

  // Find all contours
  vector<Point> cnt = cntMat;
  vector<Point> approx = cnt;

  // Get min bounding rotated rect
  for(int eps=2; eps < 400; eps += 2) {
    approxPolyDP(approx, approx, eps, true);
    if(approx.size() <= 4) {
      break;
    }
  }

  if(approx.size() == 4) {
    // Reorder points to avoid jumps
    vector<Point> hull;
    cv::convexHull(approx, hull);
    approx.clear();

    // Find top left corner
    int min = -1;
    int best = 0;
    for(int i=0; i<4; ++i) {
      int xy = hull[i].x + hull[i].y;
      if(min == -1 || xy < best) {
        min = i;
        best = xy;
      }
    }

    for(int i=0; i<4; ++i) {
      approx.push_back(Point {
            hull[(i+min)%4].x, hull[(i+min)%4].y
          });
    }

    cntApprox = Mat(approx, true);

    return true;
  }

  return false;
}

void JNICALL
Java_com_example_cameraxapp_CVLib_resizeContour(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong resizedMatAddr,
    jlong cntAddr,
    jlong resizedCntAddr
    ) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &resizedMat = *(Mat *) resizedMatAddr;
  Mat &cnt = *(Mat *) cntAddr;
  Mat &resizedCntMat = *(Mat *) resizedCntAddr;

  // Compute ratio
  float ratio = (float)mat.rows/(float)resizedMat.rows;

  // Find all contours
  vector<Point> new_cnt = cnt;

  for(int i=0; i<new_cnt.size(); ++i) {
    new_cnt[i].x *= ratio;
    new_cnt[i].y *= ratio;
  }

  resizedCntMat = Mat(new_cnt, true);
}

void JNICALL
Java_com_example_cameraxapp_CVLib_drawContour(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong cntAddr,
    jint r, jint g, jint b
    ) {
  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &cntMat = *(Mat *) cntAddr;

  // Draw contour
  vector<Point> cnt = cntMat;
  vector<vector<Point> > cnts;
  cnts.push_back(cnt);

  drawContours(mat, cnts, 0, Scalar(r, g, b), 6);

}

void JNICALL
Java_com_example_cameraxapp_CVLib_getRotatedImage(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong resultAddr) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &result = *(Mat *) resultAddr;

  // Resize image
  rotate(mat, result, ROTATE_90_CLOCKWISE);
}

jboolean JNICALL
Java_com_example_cameraxapp_CVLib_isContourValid(JNIEnv *env,
    jobject instance,
    jlong cntAddr,
    jlong pcntAddr) {

  // get Mat from raw address
  Mat &cntMat = *(Mat *) cntAddr;
  Mat &pcntMat = *(Mat *) pcntAddr;

  if(pcntMat.rows == 0) {
    return true;
  }

  vector<Point> cnt = cntMat;
  vector<Point> pcnt = pcntMat;

  for(int i=0; i<4; ++i) {
    Point p1 = cnt[i];
    Point p2 = pcnt[i];

    int dx = p1.x - p2.x;
    int dy = p1.y - p2.y;

    if(dx*dx + dy*dy > 50*50) {
      return false;
    }
  }

  return true;
}

void JNICALL
Java_com_example_cameraxapp_CVLib_smoothenContour(JNIEnv *env,
    jobject instance,
    jlong cntAddr,
    jlong pcntAddr,
    jlong acntAddr) {

  // get Mat from raw address
  Mat &cntMat = *(Mat *) cntAddr;
  Mat &pcntMat = *(Mat *) pcntAddr;
  Mat &acntMat = *(Mat *) acntAddr;

  pcntMat = cntMat;

  if(acntMat.rows == 0) {
    acntMat = cntMat;
    return;
  }

  vector<Point> acnt = acntMat;
  vector<Point> cnt = cntMat;

  const float alpha = 0.2f;

  for(int i=0; i<4; ++i) {
    Point& pa = acnt[i];
    Point& pc = cnt[i];

    pa.x = (int)(pa.x*(1.f-alpha) + pc.x*alpha);
    pa.y = (int)(pa.y*(1.f-alpha) + pc.y*alpha);
  }

  acntMat = Mat(acnt, true);
}

void JNICALL
Java_com_example_cameraxapp_CVLib_drawContourDots(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong cntAddr,
    jint r, jint g, jint b
    ) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &cntMat = *(Mat *) cntAddr;

  vector<Point> cnt = cntMat;

  for(int i=0; i<4; ++i) {
    circle(mat, cnt[i], 40, Scalar(r, g, b), 20);
  }
}

void JNICALL
Java_com_example_cameraxapp_CVLib_doFilterEnhance(JNIEnv *env, 
    jobject instance,
    jlong matAddr,
    jlong resultAddr) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &result = *(Mat *) resultAddr;


  // Inpspired from the watercolour filter

  // First make the colours more vibrant
  cv::Mat img_hsv, adjust_v, img_soft, img_gray;
  cv::Mat adjust_s;
  cv::cvtColor(mat, img_hsv, cv::COLOR_BGR2HSV);

  cv::Mat channels[3];
  cv::split(img_hsv, channels);

  channels[2].convertTo(adjust_v, CV_16U);
  adjust_v = 3*adjust_v/2;
  adjust_v.convertTo(channels[2], CV_8U);

  channels[1].convertTo(adjust_s, CV_16U);
  adjust_s = adjust_s*2;
  adjust_s.convertTo(channels[1], CV_8U);

  cv::merge(channels, 3, img_hsv);

  cv::cvtColor(img_hsv, img_soft, cv::COLOR_HSV2BGR);

  // Then extract the mask for the text layer
  int ksize = (int)((mat.rows+mat.cols)/100);
  ksize = (ksize/2)*2 + 1; // make it odd
                           
  Mat blur, fg;
  cv::cvtColor(mat, img_gray, cv::COLOR_BGR2GRAY);
  cv::GaussianBlur(img_gray, blur, Size(3,3), 0);

  cv::adaptiveThreshold(img_gray, fg, 255, 
    cv::ADAPTIVE_THRESH_GAUSSIAN_C,
    cv::THRESH_BINARY, ksize, 8);

  cv::bitwise_not(fg, fg);
  cv::GaussianBlur(fg, fg, Size(3,3), 0);

  // Make a layer for the background
  // whiten the paper
  Scalar mean_color = cv::mean(img_soft);

  Vec3f mean3f;
  mean3f[0] = (float)mean_color.val[0];
  mean3f[1] = (float)mean_color.val[1];
  mean3f[2] = (float)mean_color.val[2];

  Mat whiten = img_soft.clone();
  whiten.forEach<Vec3b>
  (
    [&](auto& pixel, const int* position) -> void
    {
      Vec3f pixel3f = pixel;
      Vec3f dist = pixel3f - mean3f;
      float dist2 = dist.dot(dist);
      if(dist2 < 100.f*100.f) {
        pixel[0] = 255;
        pixel[1] = 255;
        pixel[2] = 255;
      }
    }
  );

  // Blend everything together
  Mat alpha;
  fg.convertTo(alpha, CV_32F);

  Mat dest(mat.rows, mat.cols, CV_8UC3);

  alpha.forEach<float>
  (
    [&](auto &pixel, const int * position) -> void
    {
      float a = (pixel/255.f);
      a = std::min(a*2.f, 1.f);
      Vec3f blended = a * (Vec3f)img_soft.at<Vec3b>(position) + (1.f - a) * (Vec3f)whiten.at<Vec3b>(position);
      Vec3b blended_u8;
      blended_u8[0] = cv::saturate_cast<uchar>(blended[0]);
      blended_u8[1] = cv::saturate_cast<uchar>(blended[1]);
      blended_u8[2] = cv::saturate_cast<uchar>(blended[2]);

      dest.at<Vec3b>(position) = blended_u8;
    }
  );

  result = dest;
}

void JNICALL
Java_com_example_cameraxapp_CVLib_doFilterGrayscale(JNIEnv *env, 
    jobject instance,
    jlong matAddr,
    jlong resultAddr) {
  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &result = *(Mat *) resultAddr;

  Mat gray;
  cv::cvtColor(mat, gray, cv::COLOR_BGR2GRAY);
  cv::cvtColor(gray, result, cv::COLOR_GRAY2BGR);
}

void JNICALL
Java_com_example_cameraxapp_CVLib_doFilterBW(JNIEnv *env, 
    jobject instance,
    jlong matAddr,
    jlong resultAddr) {
  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &result = *(Mat *) resultAddr;

  Mat gray, th;
  cv::cvtColor(mat, gray, cv::COLOR_BGR2GRAY);
  cv::threshold(gray, th, 0, 255, cv::THRESH_BINARY | cv::THRESH_OTSU);

  cv::cvtColor(th, result, cv::COLOR_GRAY2BGR);
}

void JNICALL
Java_com_example_cameraxapp_CVLib_getDocumentWarped(
    JNIEnv *env, 
    jobject instance,
    jlong matAddr,
    jlong resultAddr,
    jlong cntAddr) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &result = *(Mat *) resultAddr;
  Mat &cntMat = *(Mat *) cntAddr;

  // Find all contours
  vector<Point> cnt = cntMat;

  vector<Point2f> src_pts;
  for(int i=0; i<4; ++i) {
    src_pts.push_back(Point2f { (float)cnt[i].x, (float)cnt[i].y });
  }

  // compute width and height
  auto getLength = [](Point2f& p1, Point2f& p2) -> float {
    float dx = p1.x - p2.x;
    float dy = p1.y - p2.y;

    return sqrt(dx*dx + dy*dy);
  };

  float w1 = getLength(src_pts[0], src_pts[1]);
  float w2 = getLength(src_pts[2], src_pts[3]);
  float h1 = getLength(src_pts[1], src_pts[2]);
  float h2 = getLength(src_pts[0], src_pts[3]);

  float w = (w1+w2)/2.f;
  float h = (h1+h2)/2.f;

  vector<Point2f> dst_pts {
    Point2f { 0, 0 },
            Point2f { w, 0 },
            Point2f { w, h },
            Point2f { 0, h }
  };

  // Do perspective warping
  Mat M = cv::getPerspectiveTransform(src_pts, dst_pts);
  cv::warpPerspective(mat, result, M, Size(w, h));
}

void JNICALL
Java_com_example_cameraxapp_CVLib_doFilterSoft(JNIEnv *env, 
    jobject instance,
    jlong matAddr,
    jlong resultAddr) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &result = *(Mat *) resultAddr;

  std::random_device rd;
  std::mt19937 gen(rd());
  std::uniform_int_distribution<> dis_x(0, mat.size[1]-1);
  std::uniform_int_distribution<> dis_y(0, mat.size[0]-1);

  // Compute global background using GMM
  Ptr<ml::EM> emModel = ml::EM::create();
  emModel->setClustersNumber(3);
  emModel->setCovarianceMatrixType(ml::EM::COV_MAT_DIAGONAL);
  emModel->setTermCriteria(TermCriteria(TermCriteria::COUNT + TermCriteria::EPS, 100, 0.1));

  Mat samples(1000, 3, CV_32F);
  for(int i=0; i<samples.size[0]; ++i) {
    int x = dis_x(gen);
    int y = dis_y(gen); 

    auto vec = mat.at<Vec3b>(y,x);
    samples.at<float>(i,0) = (float)vec[0];
    samples.at<float>(i,1) = (float)vec[1];
    samples.at<float>(i,2) = (float)vec[2];
  }
  emModel->trainEM(samples);

  Mat means = emModel->getMeans();

  // Get highest value
  int bg_id = -1;
  double bg_sum = -1.0;
  for(int i=0; i<means.size[0]; ++i) {
    double sum = means.at<double>(i,0) + means.at<double>(i,1) + means.at<double>(i,2);
    if(sum > bg_sum) {
      bg_id = i;
      bg_sum = sum;
    }
  }

  Vec3f bg_color;
  bg_color[0] = (float)(means.at<double>(bg_id,0));
  bg_color[1] = (float)(means.at<double>(bg_id,1));
  bg_color[2] = (float)(means.at<double>(bg_id,2));

  // Compute Local background
  Mat smap;
  Mat kernel = getStructuringElement(MORPH_RECT, Size(21, 21));
  morphologyEx(mat, smap, MORPH_CLOSE, kernel, Point(-1, -1), 1, BORDER_REFLECT101 );

  // Compute shadow map
  Mat alpha;
  smap.convertTo(alpha, CV_32FC3);

  alpha.forEach<cv::Vec3f>
    (
     [&bg_color](auto &pixel, const int * position) -> void
     {
     pixel[0]/=bg_color[0];
     pixel[1]/=bg_color[1];
     pixel[2]/=bg_color[2];
     }
    );

  Mat src_correct;
  divide(mat, alpha, src_correct, 1, CV_8UC3);

  // Accentuate colors slightly
  Mat img_lab;
  cvtColor(src_correct, img_lab, COLOR_BGR2Lab);

  Mat channels[3];
  split(img_lab, channels);

  Mat chan_a, chan_b;
  channels[1].convertTo(chan_a, CV_16S);
  channels[2].convertTo(chan_b, CV_16S);

  double min_a, max_a;
  minMaxLoc(chan_a, &min_a, &max_a);

  double min_b, max_b;
  minMaxLoc(chan_b, &min_b, &max_b);

  int16_t a_low, a_low_new;
  int16_t a_high, a_high_new;

  int16_t b_low, b_low_new;
  int16_t b_high, b_high_new;

  a_low = (int16_t)min_a;
  a_high = (int16_t)max_a;

  int16_t inc = 2;

  a_low_new = std::max(a_low-inc, 0);
  a_high_new = std::min(a_high+inc, 255);

  b_low = (int16_t)min_b;
  b_high = (int16_t)max_b;

  b_low_new = std::max(b_low-inc, 0);
  b_high_new = std::min(b_high+inc, 255);

  // rescale
  chan_a = (chan_a-a_low)*(a_high_new-a_low_new)/(a_high-a_low) + a_low_new;
  chan_b = (chan_b-b_low)*(b_high_new-b_low_new)/(b_high-b_low) + b_low_new;

  chan_a.convertTo(channels[1], CV_8U);
  chan_b.convertTo(channels[2], CV_8U);

  merge(channels, 3, img_lab);
  cvtColor(img_lab, src_correct, COLOR_Lab2BGR);

  result = src_correct;
}

void JNICALL
Java_com_example_cameraxapp_CVLib_doFilterClear(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong resultAddr) {

  // get Mat from raw address
  Mat &src = *(Mat *) matAddr;
  Mat &result = *(Mat *) resultAddr;


  Mat img_hsv;
  cvtColor(src, img_hsv, cv::COLOR_BGR2HSV);

  Mat channels[3];
  split(img_hsv, channels);

  // ENHANCEMENT
  Mat gray, gray_eq;

  cvtColor(src, gray, COLOR_BGR2GRAY);

  GaussianBlur(gray, gray, Size(13, 13), 0);

  Mat th;
  adaptiveThreshold(gray, th, 255,
      ADAPTIVE_THRESH_GAUSSIAN_C,
      THRESH_BINARY, 11, 2);


  Mat th_sat;
  threshold(channels[1], th_sat, 30*255/100, 255, THRESH_BINARY_INV);

  bitwise_and(th, th_sat, th);


  Mat kernel = getStructuringElement(MORPH_RECT, Size(21, 21));

  morphologyEx(th, th, MORPH_OPEN, kernel);



  kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
  morphologyEx(th, th, MORPH_CLOSE, kernel);


  kernel = getStructuringElement(MORPH_RECT, Size(13, 13));
  morphologyEx(th, th, MORPH_ERODE, kernel);


  Mat blur, src_correct;
  src_correct = src.clone();
  GaussianBlur(src_correct, blur, Size(0, 0), 5);
  addWeighted(src, 1.5, blur, -0.5, 0, src_correct);

  Mat corrected = src_correct.clone();
  corrected.forEach<Vec3b>(
    [&th](auto& pixel, const int* position) {
      if(th.at<uint8_t>(position[0], position[1]) == 255) {
        pixel[0] = 255;
        pixel[1] = 255;
        pixel[2] = 255;
      }
  });


  bitwise_not(th, th);

  Mat labels, stats, centroids;
  connectedComponentsWithStats(th, labels, stats, centroids, 8);

  for(int i=1;i<stats.rows; ++i) {
    int area = stats.at<int>(i, CC_STAT_AREA);

    int x0 = stats.at<int>(i, CC_STAT_LEFT);
    int y0 = stats.at<int>(i, CC_STAT_TOP);
    int w = stats.at<int>(i, CC_STAT_WIDTH);
    int h = stats.at<int>(i, CC_STAT_HEIGHT);

    Mat roi = corrected(Range(y0,y0+h), Range(x0,x0+w));
    // imshow("roi", roi);
    // waitKey();

    Mat pixels(area, 1, CV_32FC3);
    int counter = 0;
    for(int y=y0; y<y0+h; ++y) {
      for(int x=x0; x<x0+w; ++x) {
        if(labels.at<int>(y,x) == i) {
          pixels.at<Vec3f>(counter, 0) = corrected.at<Vec3b>(y, x);
          counter++;
        }
      }
    }

    Mat kmeans_labels, kmeans_centers;
    int K = 4;
    kmeans(pixels, K, kmeans_labels,
      TermCriteria( TermCriteria::EPS+TermCriteria::COUNT, 10, 0.1),
      10, KMEANS_RANDOM_CENTERS, kmeans_centers);


    Mat dist = Mat::zeros(area, K, CV_32F);

    pixels.forEach<Vec3f>(
      [&kmeans_centers, K, &dist](auto& pixel, const int* position) {
        // Some overlapping computation but oh well
        for(int j=0; j<K; ++j) {
          float d = 0.f;
          d += pow(pixel[0] - kmeans_centers.at<float>(j,0), 2);
          d += pow(pixel[1] - kmeans_centers.at<float>(j,1), 2);
          d += pow(pixel[2] - kmeans_centers.at<float>(j,2), 2);

          dist.at<float>(position[0], j) = 1.f/(d + 1e-6f);
        }
    });


    vector<Mat> center_hsv;
    float mean_v = 0.f;
    for(int j=0; j<K; ++j) {
      Mat img(1,1,CV_8UC3);
      auto& pixel = img.at<Vec3b>(0,0);
      pixel[0] = kmeans_centers.at<float>(j,0);
      pixel[1] = kmeans_centers.at<float>(j,1);
      pixel[2] = kmeans_centers.at<float>(j,2);

      Mat img_hsv;
      cvtColor(img, img_hsv, COLOR_BGR2HSV);

      center_hsv.push_back(img_hsv);
      mean_v += (float)img_hsv.at<Vec3b>(0,0)[2];
    }

    mean_v /= (float)K;

    // Correct the colors,
    // Make white, whiter
    // Make black, blacker
    // Make other colors, more vivid
    for(int j=0; j<K; ++j) {
      Mat img_hsv = center_hsv[j];
      auto& pixel_hsv = img_hsv.at<Vec3b>(0,0);

      if(pixel_hsv[1] < 40) {
        if(pixel_hsv[2] < (uint8_t)mean_v) {
          kmeans_centers.at<float>(j,0) = 0.f;
          kmeans_centers.at<float>(j,1) = 0.f;
          kmeans_centers.at<float>(j,2) = 0.f;

        } else {
          kmeans_centers.at<float>(j,0) = 255.f;
          kmeans_centers.at<float>(j,1) = 255.f;
          kmeans_centers.at<float>(j,2) = 255.f;
        }
      } else {
        float adjust_v = pixel_hsv[2];
        float adjust_s = pixel_hsv[1];
        adjust_v *= 2.f;
        adjust_s *= 2.f;
        if(adjust_v > 255.f) { adjust_v = 255.f; }
        if(adjust_s > 255.f) { adjust_s = 255.f; }
        pixel_hsv[1] = (uint8_t)adjust_s;
        pixel_hsv[2] = (uint8_t)adjust_v;

        Mat img;
        cvtColor(img_hsv, img, COLOR_HSV2BGR);

        auto& pixel_new = img.at<Vec3b>(0,0);

        kmeans_centers.at<float>(j,0) = pixel_new[0];
        kmeans_centers.at<float>(j,1) = pixel_new[1];
        kmeans_centers.at<float>(j,2) = pixel_new[2];
      }
    }

    Mat sum;
    reduce(dist, sum, 1, REDUCE_SUM, CV_32F);

    counter = 0;
    for(int y=y0; y<y0+h; ++y) {
      for(int x=x0; x<x0+w; ++x) {
        if(labels.at<int>(y,x) == i) {
          auto& pixel = corrected.at<Vec3b>(y, x);

          Vec3f result;
          result[0] = 0.f;
          result[1] = 0.f;
          result[2] = 0.f;

          float sum_i = sum.at<float>(counter, 0);

          for(int j=0; j<K; ++j) {
            result[0] += (dist.at<float>(counter,j)/sum_i) * kmeans_centers.at<float>(j,0);
            result[1] += (dist.at<float>(counter,j)/sum_i) * kmeans_centers.at<float>(j,1);
            result[2] += (dist.at<float>(counter,j)/sum_i) * kmeans_centers.at<float>(j,2);
          }

          pixel[0] = (uint8_t)result[0];
          pixel[1] = (uint8_t)result[1];
          pixel[2] = (uint8_t)result[2];
          counter++;
        }
      }
    }
  }
  result = corrected;
}

void JNICALL
Java_com_example_cameraxapp_CVLib_addContour(JNIEnv *env,
    jobject instance,
    jlong cntAddr,
    jlong acntAddr) {

  // get Mat from raw address
  Mat &cntMat = *(Mat *) cntAddr;
  Mat &acntMat = *(Mat *) acntAddr;

  if(acntMat.rows == 0) {
    acntMat = cntMat.clone();
    return;
  }

  vector<Point> acnt = acntMat;
  vector<Point> cnt = cntMat;

  for(int i=0; i<4; ++i) {
    Point& pa = acnt[i];
    Point& pc = cnt[i];

    pa.x += pc.x;
    pa.y += pc.y;
  }

  acntMat = Mat(acnt, true);
}

void JNICALL
Java_com_example_cameraxapp_CVLib_divContour(JNIEnv *env,
    jobject instance,
    jlong acntAddr,
    jlong bcntAddr,
    jint count) {

  // get Mat from raw address
  Mat &acntMat = *(Mat *) acntAddr;
  Mat &bcntMat = *(Mat *) bcntAddr;

  vector<Point> acnt = acntMat;
  vector<Point> bcnt(4);

  for(int i=0; i<4; ++i) {
    Point& pa = acnt[i];
    Point& pb = bcnt[i];

    pb.x = pa.x/count;
    pb.y = pa.y/count;
  }

  bcntMat = Mat(bcnt, true);
}

void JNICALL
Java_com_example_cameraxapp_CVLib_drawContourLines(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong cntAddr,
    jint r, jint g, jint b
    ) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &cntMat = *(Mat *) cntAddr;

  vector<Point> cnt = cntMat;

  for(int i=0; i<4; ++i) {
    Point pt1 = cnt[i];
    Point pt2 = cnt[(i+1)%4];
      line(mat, pt1, pt2, Scalar(r, g, b), 10, LINE_AA);
  }
}

void JNICALL
Java_com_example_cameraxapp_CVLib_splitBook(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong page1Addr,
    jlong page2Addr) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;

  Mat &page1 = *(Mat *) page1Addr;
  Mat &page2 = *(Mat *) page2Addr;

  // Portrait mode
  if(mat.rows > mat.cols) {
    page1 = mat.rowRange(Range(0, mat.rows/2)).clone();
    page2 = mat.rowRange(Range(mat.rows/2, mat.rows)).clone();


    // Might also need to rotate the other, but the UI
    // can take care of that.
    rotate(page1, page1, ROTATE_90_COUNTERCLOCKWISE);
    rotate(page2, page2, ROTATE_90_COUNTERCLOCKWISE);
  // Landscape mode
  } else {
    page1 = mat.colRange(Range(0, mat.cols/2)).clone();
    page2 = mat.colRange(Range(mat.cols/2, mat.cols)).clone();
  }
}

void JNICALL
Java_com_example_cameraxapp_CVLib_createWhiteBackground(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jint width, jint height) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;

  mat = Mat::zeros(height, width, CV_8UC3);
  mat = Scalar(255, 255, 255);
}

void JNICALL
Java_com_example_cameraxapp_CVLib_blitImage(JNIEnv *env,
    jobject instance,
    jlong matAddr,
    jlong srcAddr,
    jint centerx, jint centery,
    jint sizex, jint sizey) {

  // get Mat from raw address
  Mat &mat = *(Mat *) matAddr;
  Mat &src = *(Mat *) srcAddr;

  Mat resized;
  resize(src, resized, Size(sizex, sizey), 0, 0, INTER_AREA);

  int x0 = centerx - sizex/2;
  int y0 = centery - sizey/2;

  resized.forEach<Vec3b>
  (
    [&](auto& pixel, const int* position) -> void
    {
      int off[2];
      off[0] = position[0] + x0;
      off[1] = position[1] + y0;

      if(off[0] >= 0 && off[0] < mat.cols && 
          off[1] >= 0 && off[1] < mat.rows) {
        mat.at<Vec3b>(off) = pixel;
      }
    }
  );
}

}

