package com.characterfinder.data

import com.characterfinder.data.remote.CharacterDto
import com.characterfinder.data.remote.LocationRefDto
import com.characterfinder.domain.model.Character

internal fun characterStub(id: Int, name: String) = Character(
    id = id,
    name = name,
    status = "Alive",
    species = "Human",
    gender = "Male",
    imageUrl = "https://example.com/$id.jpeg",
    origin = "Earth (C-137)",
    lastLocation = "Citadel of Ricks",
    episodeCount = 51,
)

internal fun characterDtoStub(id: Int, name: String) = CharacterDto(
    id = id,
    name = name,
    status = "Alive",
    species = "Human",
    gender = "Male",
    image = "https://example.com/$id.jpeg",
    origin = LocationRefDto("Earth (C-137)"),
    location = LocationRefDto("Citadel of Ricks"),
    episode = listOf("https://example.com/episode/1"),
)
