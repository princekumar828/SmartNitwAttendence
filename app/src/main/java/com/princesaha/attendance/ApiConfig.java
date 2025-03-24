package com.princesaha.attendance;

public class ApiConfig {
    // Base URL (Change this to your actual server URL)
    public static final String BASE_URL = "https://a2c4-2409-408a-b16-6c5b-d919-1552-f755-ec87.ngrok-free.app/";

    // API Endpoints
    public static final String  REGISTER_STUDENT_API= BASE_URL + "register_student";
    public static final String  GET_STUDENT_IMAGE_API = BASE_URL + "get_student_image/";
    public static final String  FACE_RECOGNITION_API = BASE_URL + "recognize_student";

}