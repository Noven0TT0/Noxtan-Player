package com.noxtan.player.di

import com.noxtan.player.features.local.viewmodel.LocalVideoViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val ViewModelModule = module {

  viewModelOf(::LocalVideoViewModel)
}
