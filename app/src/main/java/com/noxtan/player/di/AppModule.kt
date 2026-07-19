package com.noxtan.player.di

import kotlinx.serialization.json.Json
import org.koin.dsl.module

val AppModule = module {
  single {
    Json {
      isLenient = true
      ignoreUnknownKeys = true
    }
  }
}
