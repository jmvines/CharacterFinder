package com.characterfinder.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RickAndMortyApi {

    @GET("character")
    suspend fun searchCharacters(
        @Query("name") name: String,
        // Retrofit omite el parámetro cuando es null: sin filtro no se añade &status= a la URL.
        @Query("status") status: String? = null,
    ): CharacterPageDto

    @GET("character/{id}")
    suspend fun getCharacter(@Path("id") id: Int): CharacterDto
}
