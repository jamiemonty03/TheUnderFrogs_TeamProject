import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Instruments, NewsArticle } from '../../services/instruments';

@Component({
	imports: [DatePipe],
	selector: 'app-news-footer',
	styleUrl: './news-footer.scss',
	templateUrl: './news-footer.html',
})
export class NewsFooter {
	private readonly instruments = inject(Instruments);

	readonly articles = signal<NewsArticle[]>([]);
	readonly loading = signal(true);
	readonly hasError = signal(false);

	constructor() {
		this.instruments.getNews().subscribe({
			next: (articles) => this.articles.set(articles),
			error: () => {
				this.hasError.set(true);
				this.loading.set(false);
			},
			complete: () => this.loading.set(false),
		});
	}
}