package com.characterfinder.data.di

import com.characterfinder.data.cache.SearchCache
import com.characterfinder.data.remote.RickAndMortyApi
import com.characterfinder.data.repository.CharacterRepositoryImpl
import com.characterfinder.domain.repository.CharacterRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val BASE_URL = "https://rickandmortyapi.com/api/"
private const val SEARCH_CACHE_TTL_MILLIS = 3 * 60 * 1000L

val dataModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single { OkHttpClient() }
    single<RickAndMortyApi> {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RickAndMortyApi::class.java)
    }
    single { SearchCache(ttlMillis = SEARCH_CACHE_TTL_MILLIS) }
    single<CharacterRepository> { CharacterRepositoryImpl(get(), get()) }
}
