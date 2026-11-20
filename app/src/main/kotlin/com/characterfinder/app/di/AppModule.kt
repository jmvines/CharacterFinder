package com.characterfinder.app.di

import com.characterfinder.app.ui.detail.DetailViewModel
import com.characterfinder.app.ui.search.SearchViewModel
import com.characterfinder.domain.usecase.GetCharacterDetailUseCase
import com.characterfinder.domain.usecase.SearchCharactersUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    factory { SearchCharactersUseCase(get()) }
    factory { GetCharacterDetailUseCase(get()) }

    viewModel { SearchViewModel(get()) }
    viewModel { (characterId: Int) -> DetailViewModel(get(), characterId) }
}
