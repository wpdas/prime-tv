package com.wpdas.primetv.data.di

import com.wpdas.primetv.data.fake.FakeHomeRepository
import com.wpdas.primetv.domain.repository.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    /** Fase 1: dados fake. Substituído pela implementação com Room nas fases 2 e 3. */
    @Binds abstract fun bindHomeRepository(impl: FakeHomeRepository): HomeRepository
}
