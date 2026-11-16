package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ElimuTrackApiService {
  @POST("auth/login")
  suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

  @GET("teacher/bootstrap")
  suspend fun fetchBootstrap(): Response<TeacherBootstrapDto>

  @POST("teacher/schemes")
  suspend fun pushSchemes(@Body schemes: List<SchemeDto>): Response<SyncAckDto>

  @POST("teacher/lesson-plans")
  suspend fun pushLessonPlans(@Body plans: List<LessonPlanDto>): Response<SyncAckDto>

  @POST("teacher/records-of-work")
  suspend fun pushRecordsOfWork(@Body records: List<RecordOfWorkDto>): Response<SyncAckDto>

  @POST("teacher/attendance")
  suspend fun pushAttendance(@Body records: List<AttendanceDto>): Response<SyncAckDto>

  @POST("teacher/marksbook")
  suspend fun pushMarks(@Body marks: List<MarkEntryDto>): Response<SyncAckDto>

  @POST("teacher/observations")
  suspend fun pushObservations(@Body observations: List<ObservationDto>): Response<SyncAckDto>
}
