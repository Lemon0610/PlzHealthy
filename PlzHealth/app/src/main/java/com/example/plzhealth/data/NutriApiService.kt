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

    // 카테고리(소분류)로 검색 — foodLv6Nm 파라미터 사용
    @GET("tn_pubr_public_nutri_process_info_api")
    suspend fun getNutriInfoByCategory(
        @Query("serviceKey") serviceKey: String,
        @Query("type") type: String = "json",
        @Query("foodLv3Nm") majorCategory: String? = null,
        @Query("foodLv5Nm") subCategory: String? = null,
        @Query("foodLv6Nm") minorCategory: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100
    ): NutriResponse
}