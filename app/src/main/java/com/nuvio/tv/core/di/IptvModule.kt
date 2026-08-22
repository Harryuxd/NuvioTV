package com.nuvio.tv.core.di

import com.nuvio.tv.data.repository.IptvEpgRepositoryImpl
import com.nuvio.tv.data.repository.IptvRepositoryImpl
import com.nuvio.tv.domain.repository.IptvEpgRepository
import com.nuvio.tv.domain.repository.IptvRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IptvBindingModule {

    @Binds
    @Singleton
    abstract fun bindIptvRepository(impl: IptvRepositoryImpl): IptvRepository

    @Binds
    @Singleton
    abstract fun bindIptvEpgRepository(impl: IptvEpgRepositoryImpl): IptvEpgRepository
}
