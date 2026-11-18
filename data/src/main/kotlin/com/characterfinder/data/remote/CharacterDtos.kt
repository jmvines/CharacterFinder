package com.characterfinder.data.remote

import com.characterfinder.domain.model.Character
import kotlinx.serialization.Serializable

@Serializable
data class CharacterPageDto(
    val results: List<CharacterDto> = emptyList(),
)

@Serializable
data class CharacterDto(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val gender: String,
    val image: String,
    val origin: LocationRefDto,
    val location: LocationRefDto,
    val episode: List<String> = emptyList(),
)

@Serializable
data class LocationRefDto(
    val name: String,
)

fun CharacterDto.toDomain() = Character(
    id = id,
    name = name,
    status = status,
    species = species,
    gender = gender,
    imageUrl = image,
    origin = origin.name,
    lastLocation = location.name,
    episodeCount = episode.size,
)
