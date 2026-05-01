package com.example.plzhealth.data

import retrofit2.http.GET
import retrofit2.http.Query

interface NutriApiService {
    @GET("tn_pubr_public_nutri_process_info_api")
    suspend fun getNutriInfo(
        @Query("serviceKey") serviceKey: String,
        @Query("type") type: String = "json",
        @Query("foodNm") foodName: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 50
    ): NutriResponse
}