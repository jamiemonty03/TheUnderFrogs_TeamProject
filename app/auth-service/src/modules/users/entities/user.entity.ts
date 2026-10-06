import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn, UpdateDateColumn, VersionColumn } from 'typeorm';

@Entity('users')
export class User {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'varchar', length: 50, unique: true })
  username: string;

  @Column({ type: 'varchar', length: 255, unique: true })
  email: string;

  @Column({ type: 'varchar', length: 255 })
  password_hash: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  full_name: string | null;

  @Column({ type: 'text', array: true, default: () => "ARRAY['USER']" })
  roles: string[];

  @Column({ type: 'varchar', length: 32, unique: true, nullable: true })
  account_id: string | null;

  @Column({ type: 'boolean', default: true })
  is_active: boolean;

  @Column({ type: 'integer', default: 0 })
  failed_attempts: number;

  @Column({ type: 'timestamp', nullable: true })
  locked_until: Date | null;

  @VersionColumn({ default: 0 })
  version: number;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;

  @Column({ type: 'varchar', length: 100, default: 'SYSTEM' })
  updated_by: string;
}
