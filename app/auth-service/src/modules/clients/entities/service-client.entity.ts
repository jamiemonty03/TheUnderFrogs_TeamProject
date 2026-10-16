import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn, UpdateDateColumn } from 'typeorm';

@Entity('service_clients')
export class ServiceClient {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'varchar', length: 100, unique: true })
  client_id: string;

  @Column({ type: 'varchar', length: 255 })
  client_secret_hash: string;

  @Column({ type: 'text', array: true, default: () => "ARRAY['SERVICE']" })
  roles: string[];

  @Column({ type: 'boolean', default: true })
  is_active: boolean;

  @Column({ type: 'int', default: 0 })
  version: number;

  @Column({ type: 'varchar', length: 100, nullable: true })
  updated_by: string;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
