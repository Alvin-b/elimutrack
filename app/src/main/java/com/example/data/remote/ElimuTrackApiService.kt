package com.example.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ElimuTrackApiService {

  // Authentication & Session
  @POST("auth/login")
  suspend fun login(@Body request: LoginRequestDto): Response<ResponseBody>

  @POST("auth/change-login")
  suspend fun changeLogin(@Body request: ChangeLoginRequestDto): Response<ResponseBody>

  @GET("session")
  suspend fun getSession(): Response<ResponseBody>

  @POST("auth/logout")
  suspend fun logout(): Response<ResponseBody>

  // Dashboard & Learners
  @GET("dashboard")
  suspend fun getDashboard(@Query("stream") stream: String): Response<ResponseBody>

  @GET("learners")
  suspend fun getLearners(@Query("stream") stream: String? = null): Response<ResponseBody>

  @POST("learners")
  suspend fun createLearner(@Body request: LearnerCreateRequestDto): Response<ResponseBody>

  // Marks
  @GET("marks")
  suspend fun getMarks(
    @Query("stream") stream: String,
    @Query("learner") learnerId: String? = null,
  ): Response<ResponseBody>

  @PUT("marks")
  suspend fun putMarks(@Body request: MarksPutRequestDto): Response<ResponseBody>

  // Attendance (Register)
  @GET("attendance")
  suspend fun getAttendance(
    @Query("stream") stream: String,
    @Query("day") day: String? = null,
  ): Response<ResponseBody>

  @POST("attendance")
  suspend fun postAttendance(@Body request: AttendancePostRequestDto): Response<ResponseBody>

  // Observations
  @GET("observations")
  suspend fun getObservations(
    @Query("stream") stream: String,
    @Query("day") day: String? = null,
  ): Response<ResponseBody>

  @POST("observations")
  suspend fun postObservations(@Body request: ObservationPostRequestDto): Response<ResponseBody>

  // Schemes of Work
  @GET("schemes")
  suspend fun getSchemes(
    @Query("stream") stream: String? = null,
    @Query("subjectId") subjectId: String? = null,
    @Query("term") term: String? = null,
  ): Response<ResponseBody>

  @POST("schemes")
  suspend fun createSchemeRow(@Body row: SchemeRowApiDto): Response<ResponseBody>

  @POST("schemes/bulk")
  suspend fun createSchemesBulk(@Body request: SchemeBulkRequestDto): Response<ResponseBody>

  @PUT("schemes")
  suspend fun updateSchemeRow(@Body row: SchemeRowApiDto): Response<ResponseBody>

  @HTTP(method = "DELETE", path = "schemes", hasBody = true)
  suspend fun deleteSchemeRow(@Body body: DeleteIdRequestDto): Response<ResponseBody>

  // Lesson Plans
  @GET("lesson-plans")
  suspend fun getLessonPlans(): Response<ResponseBody>

  @POST("lesson-plans")
  suspend fun createLessonPlan(@Body request: LessonPlanApiRequestDto): Response<ResponseBody>

  @PUT("lesson-plans")
  suspend fun updateLessonPlan(@Body request: LessonPlanApiRequestDto): Response<ResponseBody>

  @HTTP(method = "DELETE", path = "lesson-plans", hasBody = true)
  suspend fun deleteLessonPlan(@Body body: DeleteIdRequestDto): Response<ResponseBody>

  // Record of Work
  @GET("recordwork")
  suspend fun getRecordWork(@Query("stream") stream: String): Response<ResponseBody>

  @POST("recordwork")
  suspend fun createRecordWork(@Body request: RecordWorkApiDto): Response<ResponseBody>

  @PUT("recordwork")
  suspend fun updateRecordWork(@Body request: RecordWorkApiDto): Response<ResponseBody>

  @HTTP(method = "DELETE", path = "recordwork", hasBody = true)
  suspend fun deleteRecordWork(@Body body: DeleteIdRequestDto): Response<ResponseBody>

  // Learner Progress
  @GET("progress")
  suspend fun getProgress(
    @Query("stream") stream: String,
    @Query("learner") learnerId: String? = null,
  ): Response<ResponseBody>

  @POST("progress")
  suspend fun createProgress(@Body request: ProgressPostRequestDto): Response<ResponseBody>

  @HTTP(method = "DELETE", path = "progress", hasBody = true)
  suspend fun deleteProgress(@Body body: DeleteIdRequestDto): Response<ResponseBody>

  // Timetable (Read-only)
  @GET("timetable")
  suspend fun getTimetable(): Response<ResponseBody>

  // KICD & Approved Resources
  @GET("kicd")
  suspend fun getKicd(
    @Query("grade") grade: String,
    @Query("subject") subject: String,
  ): Response<ResponseBody>

  @GET("resources")
  suspend fun getResources(
    @Query("grade") grade: String,
    @Query("subject") subject: String,
  ): Response<ResponseBody>

  // Payments
  @GET("payments")
  suspend fun getPayments(@Query("stream") stream: String): Response<ResponseBody>

  @POST("payments")
  suspend fun createPayment(@Body request: PaymentPostRequestDto): Response<ResponseBody>

  // WhatsApp
  @GET("whatsapp")
  suspend fun getWhatsApp(@Query("learnerId") learnerId: String? = null): Response<ResponseBody>

  @POST("whatsapp")
  suspend fun postWhatsApp(@Body request: WhatsAppPostRequestDto): Response<ResponseBody>

  // Broadsheet CSV
  @GET("broadsheet/csv")
  suspend fun getBroadsheetCsv(@Query("stream") stream: String): Response<ResponseBody>
}
