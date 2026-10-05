import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn, UpdateDateColumn } from 'typeorm';

@Entity('auth')
export class Auth {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'integer' })
  user_id: number;

  @Column({ type: 'varchar', length: 255, nullable: true })
  last_login: string;

  @Column({ type: 'boolean', default: true })
  is_2fa_enabled: boolean;

  @Column({ type: 'integer', default: 0 })
  failed_login_attempts: number;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
