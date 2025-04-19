#include <jni.h>
#include "eigen/Eigen/Dense"
#include <stdexcept>

using namespace Eigen;

// Helper to convert float* to row-major Eigen Matrix
Matrix<float, Dynamic, Dynamic, RowMajor> toMatrix(const jfloat* array, int rows, int cols) {
    return Map<const Matrix<float, Dynamic, Dynamic, RowMajor>>(array, rows, cols);
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_matrixcalculator_MainActivity_addMatrices(
        JNIEnv *env, jobject, jfloatArray matA, jfloatArray matB, jint rows, jint cols) {

    int size = rows * cols;
    jfloat* a = env->GetFloatArrayElements(matA, nullptr);
    jfloat* b = env->GetFloatArrayElements(matB, nullptr);

    auto A = toMatrix(a, rows, cols);
    auto B = toMatrix(b, rows, cols);
    Matrix<float, Dynamic, Dynamic, RowMajor> C = A + B;

    jfloatArray result = env->NewFloatArray(size);
    env->SetFloatArrayRegion(result, 0, size, C.data());

    env->ReleaseFloatArrayElements(matA, a, JNI_ABORT);
    env->ReleaseFloatArrayElements(matB, b, JNI_ABORT);

    return result;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_matrixcalculator_MainActivity_subtractMatrices(
        JNIEnv *env, jobject, jfloatArray matA, jfloatArray matB, jint rows, jint cols) {

    int size = rows * cols;
    jfloat* a = env->GetFloatArrayElements(matA, nullptr);
    jfloat* b = env->GetFloatArrayElements(matB, nullptr);

    auto A = toMatrix(a, rows, cols);
    auto B = toMatrix(b, rows, cols);
    Matrix<float, Dynamic, Dynamic, RowMajor> C = A - B;

    jfloatArray result = env->NewFloatArray(size);
    env->SetFloatArrayRegion(result, 0, size, C.data());

    env->ReleaseFloatArrayElements(matA, a, JNI_ABORT);
    env->ReleaseFloatArrayElements(matB, b, JNI_ABORT);

    return result;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_matrixcalculator_MainActivity_multiplyMatrices(
        JNIEnv *env, jobject, jfloatArray matA, jfloatArray matB,
        jint rowsA, jint colsA, jint colsB) {

    jfloat* a = env->GetFloatArrayElements(matA, nullptr);
    jfloat* b = env->GetFloatArrayElements(matB, nullptr);

    auto A = toMatrix(a, rowsA, colsA);
    auto B = toMatrix(b, colsA, colsB);  // colsA == rowsB
    Matrix<float, Dynamic, Dynamic, RowMajor> C = A * B;

    jfloatArray result = env->NewFloatArray(rowsA * colsB);
    env->SetFloatArrayRegion(result, 0, rowsA * colsB, C.data());

    env->ReleaseFloatArrayElements(matA, a, JNI_ABORT);
    env->ReleaseFloatArrayElements(matB, b, JNI_ABORT);

    return result;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_matrixcalculator_MainActivity_divideMatrices(
        JNIEnv *env, jobject, jfloatArray matA, jfloatArray matB, jint size) {

    jfloat* a = env->GetFloatArrayElements(matA, nullptr);
    jfloat* b = env->GetFloatArrayElements(matB, nullptr);

    auto A = toMatrix(a, size, size);
    auto B = toMatrix(b, size, size);

    jfloatArray result = env->NewFloatArray(size * size);

    if (B.determinant() == 0) {
        // Return zero array if not invertible
        jfloat* zero = new jfloat[size * size]();
        env->SetFloatArrayRegion(result, 0, size * size, zero);
        delete[] zero;
    } else {
        Matrix<float, Dynamic, Dynamic, RowMajor> C = A * B.inverse();
        env->SetFloatArrayRegion(result, 0, size * size, C.data());
    }

    env->ReleaseFloatArrayElements(matA, a, JNI_ABORT);
    env->ReleaseFloatArrayElements(matB, b, JNI_ABORT);

    return result;
}
